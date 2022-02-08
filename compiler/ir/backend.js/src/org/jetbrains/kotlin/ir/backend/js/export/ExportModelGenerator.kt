/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.export

import org.jetbrains.kotlin.backend.common.ir.isExpect
import org.jetbrains.kotlin.backend.common.ir.isMethodOfAny
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.backend.js.lower.ES6AddInternalParametersToConstructorPhase.ES6_INIT_BOX_PARAMETER
import org.jetbrains.kotlin.ir.backend.js.lower.ES6AddInternalParametersToConstructorPhase.ES6_RESULT_TYPE_PARAMETER
import org.jetbrains.kotlin.ir.backend.js.utils.getJsNameOrKotlinName
import org.jetbrains.kotlin.ir.backend.js.utils.isExportedInterface
import org.jetbrains.kotlin.ir.backend.js.utils.isJsExport
import org.jetbrains.kotlin.ir.backend.js.utils.sanitizeName
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.keysToMap

class ExportModelGenerator(
    val context: JsIrBackendContext,
    val generateNamespacesForPackages: Boolean
) {

    fun generateExport(file: IrPackageFragment): List<ExportedDeclaration> {
        val namespaceFqName = file.fqName
        val exports = file.declarations.flatMap { declaration -> listOfNotNull(exportDeclaration(declaration)) }
        return when {
            exports.isEmpty() -> emptyList()
            !generateNamespacesForPackages || namespaceFqName.isRoot -> exports
            else -> listOf(ExportedNamespace(namespaceFqName.toString(), exports))
        }
    }

    fun generateExport(modules: Iterable<IrModuleFragment>, moduleKind: ModuleKind = ModuleKind.PLAIN): ExportedModule =
        ExportedModule(
            context.configuration[CommonConfigurationKeys.MODULE_NAME]!!,
            moduleKind,
            (context.externalPackageFragment.values + modules.flatMap { it.files }).flatMap {
                generateExport(it)
            }
        )

    private fun exportDeclaration(declaration: IrDeclaration): ExportedDeclaration? {
        val candidate = getExportCandidate(declaration) ?: return null
        if (!shouldDeclarationBeExported(candidate, context)) return null

        return when (candidate) {
            is IrSimpleFunction -> exportFunction(candidate)
            is IrProperty -> exportProperty(candidate)
            is IrClass -> {
                if (candidate.isEnumClass) {
                    exportEnumClass(candidate)
                } else {
                    exportClass(candidate)
                }
            }
            is IrField -> null
            else -> error("Can't export declaration $candidate")
        }
    }

    private fun exportFunction(function: IrSimpleFunction): ExportedDeclaration? {
        return when (val exportability = functionExportability(function)) {
            is Exportability.NotNeeded -> null
            is Exportability.Prohibited -> ErrorDeclaration(exportability.reason)
            is Exportability.Allowed -> {
                val parent = function.parent
                ExportedFunction(
                    function.getExportedIdentifier(),
                    returnType = exportType(function.returnType),
                    parameters = (listOfNotNull(function.extensionReceiverParameter) + function.valueParameters).map { exportParameter(it) },
                    typeParameters = function.typeParameters.map(::exportTypeParameter),
                    isMember = parent is IrClass,
                    isStatic = function.isStaticMethodOfClass,
                    isAbstract = parent is IrClass && !parent.isInterface && function.modality == Modality.ABSTRACT,
                    isProtected = function.visibility == DescriptorVisibilities.PROTECTED,
                    ir = function
                )
            }
        }
    }

    private fun exportConstructor(constructor: IrConstructor): ExportedDeclaration? {
        if (!constructor.isPrimary) return null
        val allValueParameters = listOfNotNull(constructor.extensionReceiverParameter) +
                constructor.valueParameters.filterNot { it.origin === ES6_RESULT_TYPE_PARAMETER || it.origin === ES6_INIT_BOX_PARAMETER }
        return ExportedConstructor(
            parameters = allValueParameters.map { exportParameter(it) },
            visibility = constructor.visibility.toExportedVisibility()
        )
    }

    private fun exportParameter(parameter: IrValueParameter): ExportedParameter {
        // Parameter names do not matter in d.ts files. They can be renamed as we like
        var parameterName = sanitizeName(parameter.name.asString(), withHash = false)
        if (parameterName in allReservedWords)
            parameterName = "_$parameterName"

        return ExportedParameter(
            parameterName,
            exportType(parameter.type),
            parameter.origin == JsLoweredDeclarationOrigin.JS_SHADOWED_DEFAULT_PARAMETER
        )
    }

    private fun exportProperty(property: IrProperty): ExportedDeclaration? {
        for (accessor in listOfNotNull(property.getter, property.setter)) {
            // TODO: Report a frontend error
            if (accessor.extensionReceiverParameter != null)
                return null
            if (accessor.isFakeOverride && !accessor.isAllowedFakeOverriddenDeclaration(context)) {
                return null
            }
        }

        return exportPropertyUnsafely(property)
    }

    private fun exportPropertyUnsafely(
        property: IrProperty,
        specializeType: ExportedType? = null
    ): ExportedDeclaration {
        val parentClass = property.parent as? IrClass

        return ExportedProperty(
            property.getExportedIdentifier(),
            specializeType ?: exportType(property.getter!!.returnType),
            mutable = property.isVar,
            isMember = parentClass != null,
            isStatic = false,
            isAbstract = parentClass?.isInterface == false && property.modality == Modality.ABSTRACT,
            isProtected = property.visibility == DescriptorVisibilities.PROTECTED,
            isField = parentClass?.isInterface == true,
            irGetter = property.getter,
            irSetter = property.setter
        )
    }

    private fun exportEnumEntry(field: IrField, enumEntries: Map<IrEnumEntry, Int>): ExportedProperty {
        val irEnumEntry = context.mapping.fieldToEnumEntry[field]
            ?: error("Unable to find enum entry for ${field.fqNameWhenAvailable}")

        val parentClass = field.parent as IrClass

        val name = irEnumEntry.getExportedIdentifier()
        val ordinal = enumEntries.getValue(irEnumEntry)

        fun fakeProperty(name: String, type: ExportedType) =
            ExportedProperty(
                name = name,
                type = type,
                mutable = false,
                isMember = true,
                isStatic = false,
                isAbstract = false,
                isProtected = false,
                irGetter = null,
                irSetter = null,
                isField = false,
            )

        val nameProperty = fakeProperty(
            name = "name",
            type = ExportedType.LiteralType.StringLiteralType(name),
        )

        val ordinalProperty = fakeProperty(
            name = "ordinal",
            type = ExportedType.LiteralType.NumberLiteralType(ordinal),
        )

        val type = ExportedType.InlineInterfaceType(
            listOf(nameProperty, ordinalProperty)
        )

        return ExportedProperty(
            name = name,
            type = ExportedType.IntersectionType(exportType(parentClass.defaultType), type),
            mutable = false,
            isMember = true,
            isStatic = true,
            isAbstract = false,
            isProtected = parentClass.visibility == DescriptorVisibilities.PROTECTED,
            irGetter = context.mapping.enumEntryToGetInstanceFun[irEnumEntry]
                ?: error("Unable to find get instance fun for ${field.fqNameWhenAvailable}"),
            irSetter = null,
            isField = false,
        )
    }

    private fun classExportability(klass: IrClass): Exportability {
        when (klass.kind) {
            ClassKind.ANNOTATION_CLASS ->
                return Exportability.Prohibited("Class ${klass.fqNameWhenAvailable} with kind: ${klass.kind}")

            ClassKind.OBJECT,
            ClassKind.CLASS,
            ClassKind.INTERFACE,
            ClassKind.ENUM_CLASS,
            ClassKind.ENUM_ENTRY -> {
            }
        }

        if (klass.isSingleFieldValueClass)
            return Exportability.Prohibited("Inline class ${klass.fqNameWhenAvailable}")

        return Exportability.Allowed
    }

    private fun exportClass(
        klass: IrClass
    ): ExportedDeclaration? {
        when (val exportability = classExportability(klass)) {
            is Exportability.Prohibited -> error(exportability.reason)
            is Exportability.NotNeeded -> return null
            Exportability.Allowed -> {
            }
        }

        val (members, nestedClasses) = exportClassDeclarations(klass)

        return exportClass(
            klass,
            members,
            nestedClasses
        )
    }

    private fun exportEnumClass(
        klass: IrClass
    ): ExportedDeclaration? {
        when (val exportability = classExportability(klass)) {
            is Exportability.Prohibited -> error(exportability.reason)
            is Exportability.NotNeeded -> return null
            Exportability.Allowed -> {
            }
        }

        val enumEntries = klass
            .declarations
            .filterIsInstance<IrField>()
            .mapNotNull { context.mapping.fieldToEnumEntry[it] }

        val enumEntriesToOrdinal: Map<IrEnumEntry, Int> =
            enumEntries
                .keysToMap(enumEntries::indexOf)

        val (members, nestedClasses) = exportClassDeclarations(klass) { candidate ->
            val enumExportedMember = exportAsEnumMember(candidate, enumEntriesToOrdinal)
            enumExportedMember
        }

        val privateConstructor = ExportedConstructor(
            parameters = emptyList(),
            visibility = ExportedVisibility.PRIVATE
        )

        return exportClass(
            klass,
            listOf(privateConstructor) + members,
            nestedClasses
        ).let {
            (it as ExportedClass).copy(
                isAbstract = true,
            )
        }
    }

    private fun exportClassDeclarations(
        klass: IrClass,
        specialProcessing: (IrDeclarationWithName) -> ExportedDeclaration? = { null }
    ): ExportedClassDeclarationsInfo {
        val members = mutableListOf<ExportedDeclaration>()
        val nestedClasses = mutableListOf<ExportedClass>()

        for (declaration in klass.declarations) {
            val candidate = getExportCandidate(declaration) ?: continue
            if (!shouldDeclarationBeExported(candidate, context)) continue

            val processingResult = specialProcessing(candidate)
            if (processingResult != null) {
                members.add(processingResult)
                continue
            }

            when (candidate) {
                is IrSimpleFunction ->
                    members.addIfNotNull(exportFunction(candidate))

                is IrConstructor ->
                    members.addIfNotNull(exportConstructor(candidate))

                is IrProperty ->
                    members.addIfNotNull(exportProperty(candidate))

                is IrClass -> {
                    val ec = exportClass(candidate)
                    if (ec is ExportedClass) {
                        nestedClasses.add(ec)
                    } else {
                        members.addIfNotNull(ec)
                    }
                }

                is IrField -> {
                    assert(
                        candidate.origin == IrDeclarationOrigin.FIELD_FOR_OBJECT_INSTANCE ||
                                candidate.origin == IrDeclarationOrigin.FIELD_FOR_OUTER_THIS ||
                                candidate.correspondingPropertySymbol != null
                    ) {
                        "Unexpected field without property ${candidate.fqNameWhenAvailable}"
                    }
                }

                else -> error("Can't export member declaration $declaration")
            }
        }

        return ExportedClassDeclarationsInfo(
            members,
            nestedClasses
        )
    }

    private fun exportClass(
        klass: IrClass,
        members: List<ExportedDeclaration>,
        nestedClasses: List<ExportedClass>
    ): ExportedDeclaration {
        val typeParameters = klass.typeParameters.map { it.name.identifier }

        // TODO: Handle non-exported super types

        val superType = klass.superTypes
            .firstOrNull { !it.classifierOrFail.isInterface && it.canBeUsedAsSuperTypeOfExportedClasses() }
            ?.let { exportType(it).takeIf { it !is ExportedType.ErrorType } }

        val superInterfaces = klass.superTypes
            .filter { it.classifierOrFail.isInterface }
            .map { exportType(it) }
            .filter { it !is ExportedType.ErrorType }

        val name = klass.getExportedIdentifier()

        val exportedClass = ExportedClass(
            name = name,
            isInterface = klass.isInterface,
            isAbstract = klass.modality == Modality.ABSTRACT,
            superClass = superType,
            superInterfaces = superInterfaces,
            typeParameters = typeParameters,
            members = members,
            nestedClasses = nestedClasses,
            ir = klass
        )

        if (klass.kind == ClassKind.OBJECT) {
            var t: ExportedType = ExportedType.InlineInterfaceType(members + nestedClasses)
            if (superType != null)
                t = ExportedType.IntersectionType(t, superType)

            for (superInterface in superInterfaces) {
                t = ExportedType.IntersectionType(t, superInterface)
            }

            return ExportedProperty(
                name = name,
                type = t,
                mutable = false,
                isMember = klass.parent is IrClass,
                isStatic = !klass.isInner,
                isAbstract = false,
                isProtected = klass.visibility == DescriptorVisibilities.PROTECTED,
                irGetter = context.mapping.objectToGetInstanceFunction[klass]!!,
                irSetter = null,
                exportedObject = exportedClass,
                isField = false,
            )
        }

        return exportedClass
    }

    private fun exportAsEnumMember(
        candidate: IrDeclarationWithName,
        enumEntriesToOrdinal: Map<IrEnumEntry, Int>
    ): ExportedDeclaration? {
        val enumEntries = enumEntriesToOrdinal.keys
        return when (candidate) {
            is IrProperty -> {
                if (candidate.isAllowedFakeOverriddenDeclaration(context)) {
                    val type: ExportedType? = when (candidate.getExportedIdentifier()) {
                        "name" -> enumEntries
                            .map { it.getExportedIdentifier() }
                            .map { ExportedType.LiteralType.StringLiteralType(it) }
                            .reduce { acc: ExportedType, s: ExportedType -> ExportedType.UnionType(acc, s) }
                        "ordinal" -> enumEntriesToOrdinal
                            .map { (_, ordinal) -> ExportedType.LiteralType.NumberLiteralType(ordinal) }
                            .reduce { acc: ExportedType, s: ExportedType -> ExportedType.UnionType(acc, s) }
                        else -> null
                    }
                    exportPropertyUnsafely(
                        candidate,
                        type
                    )
                } else null
            }

            is IrField -> {
                if (candidate.origin == IrDeclarationOrigin.FIELD_FOR_ENUM_ENTRY) {
                    exportEnumEntry(candidate, enumEntriesToOrdinal)
                } else {
                    null
                }
            }

            else -> null
        }
    }

    private fun IrType.canBeUsedAsSuperTypeOfExportedClasses(): Boolean =
        !this.isAny() && classifierOrNull != context.irBuiltIns.enumClass

    private fun exportTypeArgument(type: IrTypeArgument): ExportedType {
        if (type is IrTypeProjection)
            return exportType(type.type)

        if (type is IrType)
            return exportType(type)

        return ExportedType.ErrorType("UnknownType ${type.render()}")
    }

    private fun exportTypeParameter(typeParameter: IrTypeParameter): ExportedType.TypeParameter {
        val constraint = typeParameter.superTypes.asSequence()
            .filter { it != context.irBuiltIns.anyNType }
            .map(::exportType)
            .filter { it !is ExportedType.ErrorType && it !is ExportedType.ImplicitlyExportedType }
            .toList()

        return ExportedType.TypeParameter(
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

            // TODO: Cover these in frontend
            nonNullType.isBooleanArray() -> ExportedType.ErrorType("BooleanArray")
            nonNullType.isLongArray() -> ExportedType.ErrorType("LongArray")
            nonNullType.isCharArray() -> ExportedType.ErrorType("CharArray")

            nonNullType.isString() -> ExportedType.Primitive.String
            nonNullType.isThrowable() -> ExportedType.Primitive.Throwable
            nonNullType.isAny() -> ExportedType.Primitive.Any  // TODO: Should we wrap Any in a Nullable type?
            nonNullType.isUnit() -> ExportedType.Primitive.Unit
            nonNullType.isNothing() -> ExportedType.Primitive.Nothing
            nonNullType.isArray() -> ExportedType.Array(exportTypeArgument(nonNullType.arguments[0]))
            nonNullType.isSuspendFunction() -> ExportedType.ErrorType("Suspend functions are not supported")
            nonNullType.isFunction() -> ExportedType.Function(
                parameterTypes = nonNullType.arguments.dropLast(1).map { exportTypeArgument(it) },
                returnType = exportTypeArgument(nonNullType.arguments.last())
            )

            classifier is IrTypeParameterSymbol -> ExportedType.TypeParameter(classifier.owner.name.identifier)

            classifier is IrClassSymbol -> {
                val klass = classifier.owner
                val isImplicitlyExported = !klass.isExported(context)
                val name = if (generateNamespacesForPackages) klass.fqNameWhenAvailable!!.asString() else klass.name.asString()

                when (klass.kind) {
                    ClassKind.ANNOTATION_CLASS,
                    ClassKind.ENUM_ENTRY ->
                        ExportedType.ErrorType("Class $name with kind: ${klass.kind}")

                    ClassKind.OBJECT ->
                        ExportedType.TypeOf(name)

                    ClassKind.CLASS,
                    ClassKind.ENUM_CLASS,
                    ClassKind.INTERFACE -> ExportedType.ClassType(
                        name,
                        type.arguments.map { exportTypeArgument(it) },
                        klass
                    )
                }.withImplicitlyExported(isImplicitlyExported)
            }

            else -> error("Unexpected classifier $classifier")
        }

        return exportedType.withNullability(isNullable)
    }

    private fun IrDeclarationWithName.getExportedIdentifier(): String =
        with(getJsNameOrKotlinName()) {
            if (isSpecial)
                error("Cannot export special name: ${name.asString()} for declaration $fqNameWhenAvailable")
            else identifier
        }

    private fun functionExportability(function: IrSimpleFunction): Exportability {
        if (function.isInline && function.typeParameters.any { it.isReified })
            return Exportability.Prohibited("Inline reified function")
        if (function.isSuspend)
            return Exportability.Prohibited("Suspend function")
        if (function.isFakeOverride && !function.isAllowedFakeOverriddenDeclaration(context))
            return Exportability.NotNeeded
        if (function.origin == JsLoweredDeclarationOrigin.BRIDGE_WITHOUT_STABLE_NAME ||
            function.origin == JsLoweredDeclarationOrigin.BRIDGE_WITH_STABLE_NAME ||
            function.origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER ||
            function.origin == JsLoweredDeclarationOrigin.OBJECT_GET_INSTANCE_FUNCTION ||
            function.origin == JsLoweredDeclarationOrigin.JS_SHADOWED_EXPORT ||
            function.origin == JsLoweredDeclarationOrigin.ENUM_GET_INSTANCE_FUNCTION
        ) {
            return Exportability.NotNeeded
        }

        val parentClass = function.parent as? IrClass

        if (parentClass != null && context.mapping.enumClassToInitEntryInstancesFun[parentClass] == function) {
            return Exportability.NotNeeded
        }

        val nameString = function.name.asString()
        if (nameString.endsWith("-impl"))
            return Exportability.NotNeeded


        // Workaround in case IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER is rewritten.
        // TODO: Properly fix KT-41613
        if (nameString.endsWith("\$") && function.valueParameters.any { "\$mask" in it.name.asString() }) {
            return Exportability.NotNeeded
        }

        val name = function.getExportedIdentifier()
        // TODO: Use [] syntax instead of prohibiting
        if (name in allReservedWords)
            return Exportability.Prohibited("Name is a reserved word")

        return Exportability.Allowed
    }
}

sealed class Exportability {
    object Allowed : Exportability()
    object NotNeeded : Exportability()
    class Prohibited(val reason: String) : Exportability()
}

private class ExportedClassDeclarationsInfo(
    val members: List<ExportedDeclaration>,
    val nestedClasses: List<ExportedClass>
) {
    operator fun component1() = members
    operator fun component2() = nestedClasses
}

private val IrClassifierSymbol.isInterface
    get() = (owner as? IrClass)?.isInterface == true

private fun getExportCandidate(declaration: IrDeclaration): IrDeclarationWithName? {
    // Only actual public declarations with name can be exported
    if (declaration !is IrDeclarationWithVisibility ||
        declaration !is IrDeclarationWithName ||
        !declaration.visibility.isPublicAPI ||
        declaration.isExpect
    ) {
        return null
    }

    // Workaround to get property declarations instead of its lowered accessors.
    if (declaration is IrSimpleFunction) {
        val property = declaration.correspondingPropertySymbol?.owner
        if (property != null) {
            // Return property for getter accessors only to prevent
            // returning it twice (for getter and setter) in the same scope
            return if (property.getter == declaration)
                property
            else
                null
        }
    }

    return declaration
}

private fun shouldDeclarationBeExported(declaration: IrDeclarationWithName, context: JsIrBackendContext): Boolean {
    if (context.additionalExportedDeclarationNames.contains(declaration.fqNameWhenAvailable))
        return true

    if (context.additionalExportedDeclarations.contains(declaration))
        return true

    if (declaration is IrOverridableDeclaration<*>) {
        val overriddenNonEmpty = declaration
            .overriddenSymbols
            .isNotEmpty()

        if (overriddenNonEmpty) {
            return declaration.isOverriddenExported(context) ||
                    (declaration as? IrSimpleFunction)?.isMethodOfAny() == true // Handle names for special functions
                    || declaration.isAllowedFakeOverriddenDeclaration(context)
        }
    }

    if (declaration.isJsExport())
        return true

    return when (val parent = declaration.parent) {
        is IrDeclarationWithName -> shouldDeclarationBeExported(parent, context)
        is IrAnnotationContainer -> parent.isJsExport()
        else -> false
    }
}

fun IrOverridableDeclaration<*>.isAllowedFakeOverriddenDeclaration(context: JsIrBackendContext): Boolean {
    if (this.resolveFakeOverride(allowAbstract = true)?.parentClassOrNull.isExportedInterface()) {
        return true
    }

    return overriddenSymbols
        .asSequence()
        .map { it.owner }
        .filterIsInstance<IrOverridableDeclaration<*>>()
        .filter { it.overriddenSymbols.isEmpty() }
        .mapNotNull { it.parentClassOrNull }
        .map { it.symbol }
        .any { it == context.irBuiltIns.enumClass }
}

fun IrOverridableDeclaration<*>.isOverriddenExported(context: JsIrBackendContext): Boolean =
    overriddenSymbols
        .any { shouldDeclarationBeExported(it.owner as IrDeclarationWithName, context) }

fun IrDeclaration.isExported(context: JsIrBackendContext): Boolean {
    val candidate = getExportCandidate(this) ?: return false
    return shouldDeclarationBeExported(candidate, context)
}

private fun DescriptorVisibility.toExportedVisibility() =
    when (this) {
        DescriptorVisibilities.PROTECTED -> ExportedVisibility.PROTECTED
        else -> ExportedVisibility.DEFAULT
    }

private val reservedWords = setOf(
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

private val allReservedWords = reservedWords + strictModeReservedWords
