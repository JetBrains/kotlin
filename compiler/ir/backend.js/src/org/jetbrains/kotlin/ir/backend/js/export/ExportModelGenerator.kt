/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.export

import org.jetbrains.kotlin.backend.common.ir.isExpect
import org.jetbrains.kotlin.backend.common.ir.isMethodOfAny
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.utils.getJsNameOrKotlinName
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
import org.jetbrains.kotlin.utils.addIfNotNull

class ExportModelGenerator(val context: JsIrBackendContext) {

    private fun generateExport(file: IrPackageFragment): List<ExportedDeclaration> {
        val namespaceFqName = file.fqName
        val exports = file.declarations.flatMap { declaration -> listOfNotNull(exportDeclaration(declaration)) }
        return when {
            exports.isEmpty() -> emptyList()
            namespaceFqName.isRoot -> exports
            else -> listOf(ExportedNamespace(namespaceFqName.toString(), exports))
        }
    }

    fun generateExport(module: IrModuleFragment): ExportedModule =
        ExportedModule(
            sanitizeName(context.configuration[CommonConfigurationKeys.MODULE_NAME]!!),
            (context.externalPackageFragment.values + module.files).flatMap {
                generateExport(it)
            }
        )

    private fun exportDeclaration(declaration: IrDeclaration): ExportedDeclaration? {
        val candidate = getExportCandidate(declaration) ?: return null
        if (!shouldDeclarationBeExported(candidate, context)) return null

        return when (candidate) {
            is IrSimpleFunction -> exportFunction(candidate)
            is IrProperty -> exportProperty(candidate)
            is IrClass -> exportClass(candidate)
            is IrField -> null
            else -> error("Can't export declaration $candidate")
        }
    }

    private fun exportFunction(function: IrSimpleFunction): ExportedDeclaration? =
        when (val exportability = functionExportability(function)) {
            is Exportability.NotNeeded -> null
            is Exportability.Prohibited -> ErrorDeclaration(exportability.reason)
            is Exportability.Allowed -> {
                val parent = function.parent
                ExportedFunction(
                    function.getExportedIdentifier(),
                    returnType = exportType(function.returnType),
                    parameters = (listOfNotNull(function.extensionReceiverParameter) + function.valueParameters).map { exportParameter(it) },
                    typeParameters = function.typeParameters.map { it.name.identifier },
                    isMember = parent is IrClass,
                    isStatic = function.isStaticMethodOfClass,
                    isAbstract = parent is IrClass && !parent.isInterface && function.modality == Modality.ABSTRACT,
                    ir = function
                )
            }
        }

    private fun exportConstructor(constructor: IrConstructor): ExportedDeclaration? {
        if (!constructor.isPrimary) return null
        val allValueParameters = listOfNotNull(constructor.extensionReceiverParameter) + constructor.valueParameters
        return ExportedConstructor(allValueParameters.map { exportParameter(it) })
    }

    private fun exportParameter(parameter: IrValueParameter): ExportedParameter {
        // Parameter names do not matter in d.ts files. They can be renamed as we like
        var parameterName = sanitizeName(parameter.name.asString())
        if (parameterName in allReservedWords)
            parameterName = "_$parameterName"

        return ExportedParameter(parameterName, exportType(parameter.type))
    }

    private fun exportProperty(property: IrProperty): ExportedDeclaration? {
        for (accessor in listOfNotNull(property.getter, property.setter)) {
            // TODO: Report a frontend error
            if (accessor.extensionReceiverParameter != null)
                return null
            if (accessor.isFakeOverride) {
                return null
            }
        }

        val parentClass = property.parent as? IrClass

        return ExportedProperty(
            property.getExportedIdentifier(),
            exportType(property.getter!!.returnType),
            mutable = property.isVar,
            isMember = parentClass != null,
            isStatic = false,
            isAbstract = parentClass?.isInterface == false && property.modality == Modality.ABSTRACT,
            ir = property
        )
    }

    private fun classExportability(klass: IrClass): Exportability {
        when (klass.kind) {
            ClassKind.ANNOTATION_CLASS,
            ClassKind.ENUM_CLASS,
            ClassKind.ENUM_ENTRY,
            ClassKind.OBJECT ->
                return Exportability.Prohibited("Class ${klass.fqNameWhenAvailable} with kind: ${klass.kind}")

            ClassKind.CLASS,
            ClassKind.INTERFACE -> {
            }
        }

        if (klass.isInline)
            return Exportability.Prohibited("Inline class ${klass.fqNameWhenAvailable}")

        return Exportability.Allowed
    }

    private fun exportClass(
        klass: IrClass
    ): ExportedDeclaration? {
        when (val exportability = classExportability(klass)) {
            is Exportability.Prohibited -> return ErrorDeclaration(exportability.reason)
            is Exportability.NotNeeded -> return null
        }

        val members = mutableListOf<ExportedDeclaration>()
        val statics = mutableListOf<ExportedDeclaration>()

        for (declaration in klass.declarations) {
            val candidate = getExportCandidate(declaration) ?: continue
            if (!shouldDeclarationBeExported(candidate, context)) continue

            when (candidate) {
                is IrSimpleFunction ->
                    members.addIfNotNull(exportFunction(candidate))

                is IrConstructor ->
                    members.addIfNotNull(exportConstructor(candidate))

                is IrProperty ->
                    members.addIfNotNull(exportProperty(candidate))

                is IrClass ->
                    statics.addIfNotNull(exportClass(candidate))

                is IrField -> {
                    assert(candidate.correspondingPropertySymbol != null) {
                        "Properties without fields are not supported ${candidate.fqNameWhenAvailable}"
                    }
                }

                else -> error("Can't export member declaration $declaration")
            }
        }

        val typeParameters = klass.typeParameters.map { it.name.identifier }

        // TODO: Handle non-exported super types

        val superType = klass.superTypes
            .firstOrNull { !it.classifierOrFail.isInterface && !it.isAny() }
            ?.let { exportType(it).takeIf { it !is ExportedType.ErrorType } }

        val superInterfaces = klass.superTypes
            .filter {it.classifierOrFail.isInterface }
            .map { exportType(it) }
            .filter { it !is ExportedType.ErrorType }

        val name = klass.getExportedIdentifier()

        return ExportedClass(
            name = name,
            isInterface = klass.isInterface,
            isAbstract = klass.modality == Modality.ABSTRACT,
            superClass = superType,
            superInterfaces = superInterfaces,
            typeParameters = typeParameters,
            members = members,
            statics = statics,
            ir = klass
        )
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
                when (val exportability = classExportability(klass)) {
                    is Exportability.Prohibited -> ExportedType.ErrorType(exportability.reason)
                    is Exportability.NotNeeded -> error("Not needed classes types cannot be used")
                    else -> ExportedType.ClassType(
                        klass.fqNameWhenAvailable!!.asString(),
                        type.arguments.map { exportTypeArgument(it) }
                    )
                }
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
        if (function.isFakeOverride)
            return Exportability.NotNeeded
        if (function.origin == IrDeclarationOrigin.BRIDGE ||
            function.origin == JsLoweredDeclarationOrigin.BRIDGE_TO_EXTERNAL_FUNCTION ||
            function.origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER
        ) {
            return Exportability.NotNeeded
        }

        if (function.isFakeOverriddenFromAny())
            return Exportability.NotNeeded
        if (function.name.asString().endsWith("-impl"))
            return Exportability.NotNeeded

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

private val IrClassifierSymbol.isInterface
    get() = (owner as? IrClass)?.isInterface == true

private fun getExportCandidate(declaration: IrDeclaration): IrDeclarationWithName? {
    // Only actual public declarations with name can be exported
    if (declaration !is IrDeclarationWithVisibility ||
        declaration !is IrDeclarationWithName ||
        declaration.visibility != Visibilities.PUBLIC ||
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
    if (declaration.fqNameWhenAvailable in context.additionalExportedDeclarations)
        return true

    if (declaration.isJsExport())
        return true

    return when (val parent = declaration.parent) {
        is IrDeclarationWithName -> shouldDeclarationBeExported(parent, context)
        is IrAnnotationContainer -> parent.isJsExport()
        else -> false
    }
}

fun IrDeclaration.isExported(context: JsIrBackendContext): Boolean {
    val candidate = getExportCandidate(this) ?: return false
    return shouldDeclarationBeExported(candidate, context)
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