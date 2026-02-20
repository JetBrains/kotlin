/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.export

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.backend.js.ir.getExportedIdentifier
import org.jetbrains.kotlin.ir.backend.js.tsexport.*
import org.jetbrains.kotlin.ir.backend.js.utils.getDeprecated
import org.jetbrains.kotlin.ir.backend.js.utils.getFqNameWithJsNameWhenAvailable
import org.jetbrains.kotlin.ir.backend.js.utils.isExplicitlyExported
import org.jetbrains.kotlin.ir.backend.js.utils.realOverrideTarget
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.js.config.ModuleKind
import org.jetbrains.kotlin.name.parentOrNull
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.memoryOptimizedFilter
import org.jetbrains.kotlin.utils.memoryOptimizedFlatMap
import org.jetbrains.kotlin.utils.memoryOptimizedMap

private const val NOT_EXPORTED_NAMESPACE = "not.exported"

class ExportModelGenerator(val context: WasmBackendContext) {
    private val excludedFromExport = setOf<IrDeclaration>(
        context.wasmSymbols.jsRelatedSymbols.jsReferenceClass.owner,
        context.wasmSymbols.jsRelatedSymbols.jsAnyType.classOrFail.owner,
        context.wasmSymbols.jsRelatedSymbols.jsNumberType.classOrFail.owner,
        context.wasmSymbols.jsRelatedSymbols.jsStringType.classOrFail.owner,
        context.wasmSymbols.jsRelatedSymbols.jsBooleanType.classOrFail.owner,
        context.wasmSymbols.jsRelatedSymbols.jsBigIntType.classOrFail.owner
    )

    private fun collectAllTheDeclarationsToExport(modules: Iterable<IrModuleFragment>): Iterable<IrDeclaration> {
        val declarationsToExport = mutableSetOf<IrDeclaration>()
        val queue = ArrayDeque<IrDeclaration>().apply {
            modules.asSequence()
                .flatMap { it.files }
                .flatMap { it.declarations }
                .filter { it.isExplicitlyExported() }
                .forEach {
                    declarationsToExport.add(it)
                    addLast(it)
                }
        }
        val declarationVisitor = object : IrVisitorVoid() {
            override fun visitFunction(declaration: IrFunction) {
                visitType(declaration.returnType)
                declaration.typeParameters.forEach(::visitTypeParameter)
                declaration.parameters.forEach {
                    if (it.kind != IrParameterKind.DispatchReceiver) {
                        visitValueParameter(it)
                    }
                }
            }

            override fun visitClass(declaration: IrClass) {
                declaration.superTypes.forEach(::visitType)
                declaration.typeParameters.forEach(::visitTypeParameter)
                declaration.declarations.forEach { it.acceptVoid(this) }
            }

            override fun visitProperty(declaration: IrProperty) {
                declaration.backingField?.let(::visitField)
                declaration.getter?.let(::visitFunction)
            }

            override fun visitField(declaration: IrField) {
                visitType(declaration.type)
            }

            override fun visitValueParameter(declaration: IrValueParameter) {
                visitType(declaration.type)
            }

            override fun visitTypeParameter(declaration: IrTypeParameter) {
                declaration.superTypes.forEach(::visitType)
            }

            private fun visitType(type: IrType) {
                if (type !is IrSimpleType) return
                val classifier = type.classifier as? IrClassSymbol ?: return
                val klass = classifier.owner
                if (!klass.isExternal || klass in excludedFromExport || klass in declarationsToExport) return
                queue.add(klass)
                declarationsToExport.add(klass)
                type.arguments.forEach { it.typeOrNull?.let(::visitType) }
            }
        }

        while (queue.isNotEmpty()) {
            val declaration = queue.removeFirst()
            declaration.acceptVoid(declarationVisitor)
        }

        return declarationsToExport
    }

    fun generateExport(modules: Iterable<IrModuleFragment>): ExportedModule =
        ExportedModule(
            context.configuration[CommonConfigurationKeys.MODULE_NAME]!!,
            ModuleKind.ES,
            collectAllTheDeclarationsToExport(modules).mapNotNull(::exportDeclaration)
        )

    private fun exportDeclaration(declaration: IrDeclaration): ExportedDeclaration? {
        return when (declaration) {
            is IrSimpleFunction -> exportFunction(declaration)
            is IrClass -> exportClass(declaration)
            else -> error("Can't export declaration $declaration")
        }?.withAttributesFor(declaration)
    }

    private fun exportFunction(function: IrSimpleFunction): ExportedFunction? =
        runIf(function.correspondingPropertySymbol == null && function.realOverrideTarget.parentClassOrNull?.symbol != context.irBuiltIns.anyClass) {
            val parentClass = function.parentClassOrNull
            ExportedFunction(
                ExportedMemberName.Identifier(function.getExportedIdentifier()),
                returnType = exportType(function.returnType),
                typeParameters = function.typeParameters.memoryOptimizedMap(::exportTypeParameter),
                isMember = parentClass != null,
                isStatic = function.isStaticMethodOfClass,
                isProtected = function.visibility == DescriptorVisibilities.PROTECTED,
                isAbstract = parentClass != null && !parentClass.isInterface && function.modality == Modality.ABSTRACT,
                parameters = function.parameters.filter { it.kind != IrParameterKind.DispatchReceiver }
                    .memoryOptimizedMap { exportParameter(it) },
            )
        }

    private fun exportConstructor(constructor: IrConstructor): ExportedDeclaration {
        assert(constructor.isPrimary) { "Can't export not-primary constructor" }
        val allValueParameters = constructor.parameters.filter { it.kind != IrParameterKind.DispatchReceiver }
        return ExportedConstructor(
            parameters = allValueParameters.memoryOptimizedMap { exportParameter(it) },
            visibility = constructor.visibility.toExportedVisibility()
        )
    }

    private fun exportProperty(
        property: IrProperty,
        specializeType: ExportedType? = null
    ): List<ExportedDeclaration> {
        val parentClass = property.parent as? IrClass
        val isOptional = parentClass != null &&
                property.getter?.returnType?.isNullable() == true

        val name = ExportedMemberName.Identifier(property.getExportedIdentifier())
        val propertyType = specializeType ?: exportType(property.getter!!.returnType)
        val isStatic = (property.getter ?: property.setter)?.isStaticMethodOfClass == true
        val isAbstract = parentClass?.isInterface == false && property.modality == Modality.ABSTRACT
        val isProtected = property.visibility == DescriptorVisibilities.PROTECTED
        if (parentClass?.isInterface == true) {
            return listOf(
                ExportedField(
                    name = name,
                    type = propertyType,
                    mutable = property.isVar,
                    isMember = true,
                    isAbstract = isAbstract,
                    isProtected = isProtected,
                    isObjectGetter = property.getter?.origin == JsLoweredDeclarationOrigin.OBJECT_GET_INSTANCE_FUNCTION,
                    isOptional = isOptional,
                    isStatic = isStatic,
                )
            )
        } else {
            val accessors: MutableList<ExportedDeclaration> = SmartList(
                ExportedPropertyGetter(
                    name = name,
                    type = propertyType,
                    isStatic = isStatic,
                    isAbstract = isAbstract,
                    isProtected = isProtected,
                )
            )
            if (property.isVar) {
                accessors.add(
                    ExportedPropertySetter(
                        name = name,
                        type = propertyType,
                        isStatic = isStatic,
                        isAbstract = isAbstract,
                        isProtected = isProtected,
                    )
                )
            }
            return accessors
        }
    }

    private fun exportParameter(parameter: IrValueParameter): ExportedParameter =
        ExportedParameter(
            parameter.name.asString(),
            exportType(parameter.type),
            parameter.defaultValue != null
        )

    private val currentlyProcessedTypes = hashSetOf<IrType>()

    private fun exportType(type: IrType): ExportedType {
        if (type in currentlyProcessedTypes)
            return ExportedType.Primitive.Unknown

        if (type !is IrSimpleType)
            return ExportedType.ErrorType("NonSimpleType ${type.render()}")

        currentlyProcessedTypes.add(type)

        val classifier = type.classifier
        val isMarkedNullable = type.isMarkedNullable()
        val nonNullType = type.makeNotNull() as IrSimpleType
        val jsRelatedSymbols = context.wasmSymbols.jsRelatedSymbols

        val exportedType = when {
            nonNullType.isBoolean() || nonNullType == jsRelatedSymbols.jsBooleanType -> ExportedType.Primitive.Boolean
            nonNullType.isLong() || nonNullType.isULong() || nonNullType == jsRelatedSymbols.jsBigIntType -> ExportedType.Primitive.BigInt
            nonNullType.isPrimitiveType() || nonNullType.isUByte() || nonNullType.isUShort() || nonNullType.isUInt() || nonNullType == jsRelatedSymbols.jsNumberType ->
                ExportedType.Primitive.Number
            nonNullType.isString() || nonNullType == jsRelatedSymbols.jsStringType -> ExportedType.Primitive.String
            nonNullType == jsRelatedSymbols.jsAnyType -> ExportedType.Primitive.Unknown
            nonNullType.isUnit() || nonNullType == context.wasmSymbols.voidType -> ExportedType.Primitive.Unit
            nonNullType.isFunction() -> ExportedType.Function(
                parameters = nonNullType.arguments.dropLast(1).memoryOptimizedMap {
                    ExportedParameter(
                        name = (it as? IrTypeProjection)?.type?.getAnnotationArgumentValue(StandardNames.FqNames.parameterName, "name"),
                        type = exportTypeArgument(it),
                    )
                },
                returnType = exportTypeArgument(nonNullType.arguments.last())
            )
            nonNullType.isNothing() -> ExportedType.Primitive.Nothing

            classifier is IrTypeParameterSymbol -> ExportedType.TypeParameterRef(ExportedTypeParameter(classifier.owner.name.identifier))

            classifier is IrClassSymbol -> {
                val klass = classifier.owner
                if (klass.symbol == jsRelatedSymbols.jsReferenceClass) return ExportedType.Primitive.Unknown

                require(klass.isExternal) { "Unexpected non-external class: ${klass.fqNameWhenAvailable}" }

                val name = "$NOT_EXPORTED_NAMESPACE.${klass.getFqNameWithJsNameWhenAvailable(shouldIncludePackage = true, isEsModules = true).asString()}"

                val classType = ExportedType.ClassType(
                    name = name,
                    arguments = type.arguments.memoryOptimizedMap { exportTypeArgument(it) },
                    classId = klass.classId,
                )

                when (klass.kind) {
                    ClassKind.OBJECT ->
                        ExportedType.TypeOf(classType)

                    ClassKind.CLASS,
                    ClassKind.INTERFACE,
                        ->
                        classType
                    else -> error("Unexpected class kind ${klass.kind}")
                }
            }

            else -> error("Unexpected classifier $classifier")
        }

        return exportedType.withNullability(isMarkedNullable)
            .also { currentlyProcessedTypes.remove(type) }
    }

    private fun exportTypeArgument(type: IrTypeArgument): ExportedType {
        if (type is IrTypeProjection)
            return exportType(type.type)

        if (type is IrType)
            return exportType(type)

        return ExportedType.ErrorType("UnknownType ${type.render()}")
    }

    private fun exportTypeParameter(typeParameter: IrTypeParameter): ExportedTypeParameter {
        val constraint = typeParameter.superTypes.asSequence()
            .filter { !it.isNullable() || it.makeNotNull() != context.wasmSymbols.jsRelatedSymbols.jsAnyType }
            .map { exportType(it) }
            .filter { it !is ExportedType.ErrorType }
            .toList()

        return ExportedTypeParameter(
            typeParameter.name.identifier,
            constraint.run {
                when (size) {
                    0 -> null
                    1 -> single()
                    else -> reduce(ExportedType::IntersectionType)
                }
            }
        )
    }

    private fun exportMemberDeclaration(declaration: IrDeclaration): List<ExportedDeclaration> {
        if (declaration !is IrDeclarationWithVisibility || declaration.visibility == DescriptorVisibilities.PRIVATE) return emptyList()
        return when (declaration) {
            is IrSimpleFunction -> listOfNotNull(exportFunction(declaration))
            is IrConstructor -> listOf(exportConstructor(declaration))
            is IrProperty -> exportProperty(declaration)
            else -> emptyList()
        }.map { it.withAttributesFor(declaration) }
    }

    private fun exportClass(declaration: IrClass): ExportedDeclaration {
        val typeParameters = declaration.typeParameters.memoryOptimizedMap(::exportTypeParameter)

        val superClass = declaration.superTypes
            .find { it != context.irBuiltIns.anyType && !it.classifierOrFail.isInterface }
            ?.let(::exportType)
            ?.takeIf { it !is ExportedType.ErrorType }

        val superInterfaces = declaration.superTypes
            .filter { it != context.wasmSymbols.jsRelatedSymbols.jsAnyType && it.classifierOrFail.isInterface }
            .map(::exportType)
            .memoryOptimizedFilter { it !is ExportedType.ErrorType }

        val name = declaration.getExportedIdentifier()
        val members = declaration.declarations.memoryOptimizedFlatMap(::exportMemberDeclaration)

        val exportedDeclaration = if (declaration.kind == ClassKind.OBJECT) {
            ExportedObject(
                name = name,
                members = members,
                superClasses = listOfNotNull(superClass),
                nestedClasses = emptyList(),
                superInterfaces = superInterfaces,
                originalClassId = declaration.classId,
                isExternal = declaration.isEffectivelyExternal(),
                isCompanion = declaration.isCompanion,
                isTopLevel = declaration.isTopLevel
            )
        } else {
            ExportedRegularClass(
                name = name,
                isInterface = declaration.isInterface,
                isAbstract = declaration.modality == Modality.ABSTRACT || declaration.modality == Modality.SEALED,
                isExternal = declaration.isEffectivelyExternal(),
                superClasses = listOfNotNull(superClass),
                superInterfaces = superInterfaces,
                typeParameters = typeParameters,
                members = members,
                nestedClasses = emptyList(),
                originalClassId = declaration.classId,
            )
        }

        val parentFqName = declaration.getFqNameWithJsNameWhenAvailable(shouldIncludePackage = true, isEsModules = true).parentOrNull()

        return ExportedNamespace(
            name = "$NOT_EXPORTED_NAMESPACE${parentFqName?.asString()?.takeIf { it.isNotEmpty() }?.let { ".$it" }.orEmpty()}",
            declarations = listOf(exportedDeclaration),
            isPrivate = true
        )
    }

    private val IrClassifierSymbol.isInterface
        get() = (owner as? IrClass)?.isInterface == true
}

private fun <T : ExportedDeclaration> T.withAttributesFor(declaration: IrDeclaration): T {
    declaration.getDeprecated()?.let { attributes.add(ExportedAttribute.DeprecatedAttribute(it)) }
    return this
}
