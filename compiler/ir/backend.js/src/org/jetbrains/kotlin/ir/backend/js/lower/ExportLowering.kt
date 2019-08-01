/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.isExpect
import org.jetbrains.kotlin.backend.common.ir.isMethodOfAny
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.utils.isJsExport
import org.jetbrains.kotlin.ir.backend.js.utils.sanitizeName
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe

class ExportLowering(val context: JsIrBackendContext) : FileLoweringPass {
    init {
        // println("type Nullable<T> = T | null | undefined\n")
    }

    private lateinit var currentNamespace: FqName

    override fun lower(irFile: IrFile) {
    }

    fun generateExport(irFile: IrPackageFragment): List<ExportedDeclaration> {
        val namespaceFqName = irFile.fqName
        currentNamespace = namespaceFqName
        val exports = irFile.declarations.flatMap { declaration -> exportDeclaration(declaration) }

        if (exports.isNotEmpty()) {
            // println("// Exports for file ${irFile.name}\n")

            if (namespaceFqName.isRoot) {
                return exports
            } else {
                val namespace = ExportedNamespace(namespaceFqName.toString(), exports)
                return listOf(namespace)
            }
        }

        return exports
    }

    fun generateExport(module: IrModuleFragment): ExportedModule {
        val packageFragments: List<IrPackageFragment> = context.externalPackageFragment.values + module.files

        val declarations = packageFragments.flatMap { generateExport(it) }
        return ExportedModule(declarations)
    }

    private fun exportDeclaration(declaration: IrDeclaration): List<ExportedDeclaration> {
        if (declaration !is IrDeclarationWithVisibility ||
            declaration !is IrDeclarationWithName ||
            declaration.visibility != Visibilities.PUBLIC ||
            declaration.isExpect
        ) {
            return emptyList()
        }

        if (!declaration.isExported())
            return emptyList()

        if (declaration is IrFunction && declaration.isInline && declaration.typeParameters.any { it.isReified })
            return emptyList()

        if (declaration.origin == IrDeclarationOrigin.BRIDGE)
            return emptyList()

        if (declaration.origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER)
            return emptyList()

        val exportedDeclarations = when (declaration) {
            is IrSimpleFunction -> exportFunction(declaration)
            is IrProperty -> exportProperty(declaration)
            is IrClass -> exportClass(declaration)
            is IrField -> emptyList()

            else -> error("Can't export declaration $declaration")
        }

        return exportedDeclarations
    }

    private fun exportFunction(function: IrSimpleFunction): List<ExportedDeclaration> {
//        if (function.overriddenSymbols.isNotEmpty())
//            return emptyList()

        if (function.isSuspend)
            return emptyList()


        if (function.isFakeOverride) // && function.overriddenSymbols.any { it.owner.isExported() })
            return emptyList()

        val correspondingProperty = function.correspondingPropertySymbol?.owner
        if (correspondingProperty != null) {
            if (correspondingProperty.getter == function) {
                return exportProperty(correspondingProperty)
            } else {
                // TODO: Setter only properties?
                return emptyList()
            }
        }

        if (function.isMethodOfAny())
            return emptyList()

        if (function.name.asString().endsWith("-impl"))
            return emptyList()

        // TODO: Fix properties

        val name = function.getExportedIdentifier()

        if (name[0].isUpperCase()) return emptyList()

        if (name in allReservedWords)
            return emptyList()

        if (function.fqNameWhenAvailable == FqName("kotlin.coroutines.Continuation"))
            return emptyList()

        if (function.fqNameWhenAvailable == FqName("kotlin.Comparator"))
            return emptyList()

        val returnType = exportType(function.returnType)
        val parameters = (listOfNotNull(function.extensionReceiverParameter) + function.valueParameters).map { exportParameter(it) }
        val typeParameters = function.typeParameters.map { it.name.identifier }

        val parent = function.parent

        val isMember: Boolean
        val isAbstract: Boolean
        if (parent is IrClass) {
            isAbstract = !parent.isInterface && function.modality == Modality.ABSTRACT
            isMember = true
        } else {
            isAbstract = false
            isMember = false
        }

        return listOf(
            ExportedFunction(
                name,
                returnType,
                parameters = parameters,
                typeParameters = typeParameters,
                isMember = isMember,
                isStatic = function.isStaticMethodOfClass,
                isAbstract = isAbstract
            )
        )
    }

    private fun exportConstructor(function: IrConstructor): List<ExportedDeclaration> {

        if (!function.isPrimary)
            return emptyList()

        val parameters = (listOfNotNull(function.extensionReceiverParameter) + function.valueParameters).map { exportParameter(it) }
        return listOf(ExportedConstructor(parameters, false))
    }

    private fun exportParameter(parameter: IrValueParameter): ExportedParameter {
        var parameterName = sanitizeName(parameter.name.asString())
        if (parameterName in allReservedWords)
            parameterName = "_$parameterName"

        return ExportedParameter(parameterName, exportType(parameter.type))
    }

    private fun exportProperty(property: IrProperty): List<ExportedDeclaration> {
        for (accessor in listOfNotNull(property.getter, property.setter)) {
            if (accessor.extensionReceiverParameter != null)
                return emptyList()
            if (accessor.isFakeOverride) {
                return emptyList()
            }
        }

        return listOf(
            ExportedProperty(
                property.getExportedIdentifier(),
                exportType(property.getter!!.returnType),
                mutable = property.isVar,
                isMember = property.parent is IrClass,
                isStatic = false
            )
        )
    }

    private fun exportClass(klass: IrClass): List<ExportedDeclaration> {
        if (klass.isCompanion) return emptyList()
        if (klass.isInline) return emptyList()

//        // TODO: Exceptions
        if (klass.fqNameWhenAvailable in setOf(
                FqName("kotlin.Error"),
                FqName("kotlin.AssertionError"),
                FqName("kotlin.NotImplementedError")
            ))
            return emptyList()

        val name = klass.getExportedIdentifier()

        val members = mutableListOf<ExportedDeclaration>()
        val other = mutableListOf<ExportedDeclaration>()

        for (declaration in klass.declarations) {
            if (declaration !is IrDeclarationWithVisibility ||
                declaration !is IrDeclarationWithName ||
                declaration.visibility != Visibilities.PUBLIC
            ) {
                continue
            }

            if (declaration.origin == IrDeclarationOrigin.BRIDGE)
                continue

            if (declaration.origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER)
                continue

            when (declaration) {
                is IrSimpleFunction ->
                    members += exportFunction(declaration)

                is IrConstructor ->
                    members += exportConstructor(declaration)

                is IrProperty ->
                    members += exportProperty(declaration)

                is IrClass ->
                    other += exportClass(declaration)

                is IrField -> {

                }

                else -> error("Can't export member declaration $declaration")
            }
        }

        val typeParameters = klass.typeParameters.map { it.name.identifier }

        val superType = klass.superTypes
            .firstOrNull { !it.classifierOrFail.isInterface && !it.isAny() }
            ?.let { exportType(it) }

        val superInterfaces = klass.superTypes
            .filter { it.classifierOrFail.isInterface &&
                    !it.isFunctionOrKFunction() &&
                    !it.isSuspendFunction() &&
                    !it.classifierOrFail.isClassWithFqName(FqNameUnsafe("kotlin.io.Serializable")) }
            .map { exportType(it) }

        val exportedClass = ExportedClass(
            name, klass.isInterface, klass.modality == Modality.ABSTRACT, superType, superInterfaces, typeParameters, members
        )

        val exportedNamespace = if (other.isNotEmpty()) ExportedNamespace(name, other) else null

        return listOfNotNull(exportedClass, exportedNamespace)
    }

    private fun exportTypeArgument(type: IrTypeArgument): ExportedType {
        if (type is IrTypeProjection)
            return exportType(type.type)

        if (type is IrType)
            return exportType(type)

        return ExportedType.ErrorType("UnknownType ${type.render()}")
    }

    private fun exportType(type: IrType): ExportedType {
        if (type is IrDynamicType)
            return ExportedType.Primitive.Any

        if (type !is IrSimpleType)
            return ExportedType.ErrorType("NonSimpleType ${type.render()}")

        val classifier = type.classifier
        val isNullable = type.hasQuestionMark
        val nonNullType = type.makeNotNull() as IrSimpleType

        val exportedType = when {
            nonNullType.isBoolean() -> ExportedType.Primitive.Boolean
            nonNullType.isPrimitiveType() && (!nonNullType.isLong() && !nonNullType.isChar()) ->
                ExportedType.Primitive.Number

            nonNullType.isByteArray() -> ExportedType.Primitive.ByteArray
            nonNullType.isShortArray() -> ExportedType.Primitive.ShortArray
            nonNullType.isIntArray() -> ExportedType.Primitive.IntArray
            nonNullType.isFloatArray() -> ExportedType.Primitive.FloatArray
            nonNullType.isDoubleArray() -> ExportedType.Primitive.DoubleArray

            nonNullType.isBooleanArray() -> ExportedType.ErrorType("BooleanArray")
            nonNullType.isLongArray() -> ExportedType.ErrorType("LongArray")
            nonNullType.isCharArray() -> ExportedType.ErrorType("CharArray")

            nonNullType.isString() -> ExportedType.Primitive.String
            nonNullType.isThrowable() -> ExportedType.Primitive.Throwable
            nonNullType.isAny() -> return ExportedType.Primitive.Any
            nonNullType.isUnit() -> ExportedType.Primitive.Unit
            nonNullType.isNothing() -> ExportedType.Primitive.Nothing
            nonNullType.isArray() -> ExportedType.Array(exportTypeArgument(nonNullType.arguments[0]))
            nonNullType.isSuspendFunction() -> ExportedType.ErrorType("Suspend functions are not supported")
            nonNullType.isFunction() -> ExportedType.Function(
                parameterTypes = nonNullType.arguments.dropLast(1).map { exportTypeArgument(it) },
                returnType = exportTypeArgument(nonNullType.arguments.last())
            )
            else -> {
                when (classifier) {
                    is IrTypeParameterSymbol -> ExportedType.TypeParameter(classifier.owner.name.identifier)
                    is IrClassSymbol -> {
                        val klass = classifier.owner
                        when {
                            klass.isCompanion -> ExportedType.ErrorType("CompanionObject ${klass.fqNameWhenAvailable}")
                            klass.isInline -> ExportedType.ErrorType("inline class ${klass.fqNameWhenAvailable}")
                            else ->
                                ExportedType.ClassType(
                                    shortenFqName(klass.fqNameWhenAvailable!!),
                                    type.arguments.map { exportTypeArgument(it) }
                                )
                        }
                    }
                    else -> error("Unexpected classifier $classifier")
                }
            }
        }

        return exportedType.withNullability(isNullable)
    }

    private fun shortenFqName(name: FqName): String {
        return name.asString()
        val namespaceSegments = currentNamespace.pathSegments()
        val nameSegments = name.pathSegments()

        var commonPrefixSize = 0

        for ((index, segment) in name.pathSegments().withIndex()) {
            if (index < namespaceSegments.size && segment == namespaceSegments[index]) {
                commonPrefixSize++
            } else {
                break
            }
        }

        return nameSegments
            .takeLast(nameSegments.size - commonPrefixSize)
            .joinToString(".")
    }

    private fun IrDeclarationWithName.getExportedIdentifier(): String =
        with(name /*getJsNameOrKotlinName()*/) {
            if (isSpecial)
                error("Cannot export special name: ${name.asString()} for declaration $fqNameWhenAvailable")
            else identifier
        }

    private fun IrDeclarationWithName.isExported(): Boolean {
        if (fqNameWhenAvailable in context.additionalExportedDeclarations)
            return true

        if (isJsExport())
            return true

        return when (val parent = parent) {
            is IrDeclarationWithName -> parent.isExported()
            is IrAnnotationContainer -> parent.isJsExport()
            else -> false
        }
    }
}

private val IrClassifierSymbol.isInterface get() = (owner as? IrClass)?.isInterface == true


val reservedWords = setOf(
    "break",
    "case",
    "catch",
    "class",
    "const",
    "continue",
    "debugger",
    "default",
    "delete",
    "do",
    "else",
    "enum",
    "export",
    "extends",
    "false",
    "finally",
    "for",
    "function",
    "if",
    "import",
    "in",
    "instanceof",
    "new",
    "null",
    "return",
    "super",
    "switch",
    "this",
    "throw",
    "true",
    "try",
    "typeof",
    "var",
    "void",
    "while",
    "with"
)

val strictModeReservedWords = setOf(
    "as",
    "implements",
    "interface",
    "let",
    "package",
    "private",
    "protected",
    "public",
    "static",
    "yield"
)

val allReservedWords = reservedWords + strictModeReservedWords