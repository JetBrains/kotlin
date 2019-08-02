/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

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
import org.jetbrains.kotlin.utils.addIfNotNull

class ExportGenerator(val context: JsIrBackendContext) {

    private fun generateExport(file: IrPackageFragment): List<ExportedDeclaration> {
        val namespaceFqName = file.fqName
        val exports = file.declarations.flatMap { declaration -> exportDeclaration(declaration) }
        return when {
            exports.isEmpty() -> emptyList()
            namespaceFqName.isRoot -> exports
            else -> listOf(ExportedNamespace(namespaceFqName.toString(), exports))
        }
    }

    fun generateExport(module: IrModuleFragment): ExportedModule =
        ExportedModule(
            (context.externalPackageFragment.values + module.files).flatMap {
                generateExport(it)
            }
        )

    private fun exportDeclaration(declaration: IrDeclaration): List<ExportedDeclaration> {
        if (declaration !is IrDeclarationWithVisibility ||
            declaration !is IrDeclarationWithName ||
            declaration.visibility != Visibilities.PUBLIC ||
            declaration.isExpect
        ) {
            return emptyList()
        }

        if (!shouldDeclarationBeExported(declaration))
            return emptyList()

        return when (declaration) {
            is IrSimpleFunction -> when {
                declaration.correspondingPropertySymbol != null ->
                    listOfNotNull(exportPropertyByAccessor(declaration))
                else ->
                    listOfNotNull(exportFunction(declaration))
            }
            is IrProperty -> listOfNotNull(exportProperty(declaration))
            is IrClass -> exportClass(declaration)
            is IrField -> emptyList()
            else -> error("Can't export declaration $declaration")
        }
    }

    private fun exportPropertyByAccessor(function: IrSimpleFunction): ExportedDeclaration? {
        return null
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
                    isAbstract = parent is IrClass && !parent.isInterface && function.modality == Modality.ABSTRACT
                )
            }
        }

    private fun exportConstructor(constructor: IrConstructor): ExportedDeclaration? {
        if (!constructor.isPrimary) return null
        val allValueParameters = listOfNotNull(constructor.extensionReceiverParameter) + constructor.valueParameters
        return ExportedConstructor(
            parameters = allValueParameters.map { exportParameter(it) },
            isPrivate = false
        )
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
            if (accessor.extensionReceiverParameter != null)
                return null
            if (accessor.isFakeOverride) {
                return null
            }
        }

        return ExportedProperty(
            property.getExportedIdentifier(),
            exportType(property.getter!!.returnType),
            mutable = property.isVar,
            isMember = property.parent is IrClass,
            isStatic = false
        )
    }

    private fun exportClass(klass: IrClass): List<ExportedDeclaration> {
        if (klass.isCompanion) return emptyList()
        if (klass.isInline) return emptyList()

        // TODO: Exceptions
        if (klass.fqNameWhenAvailable in setOf(
                FqName("kotlin.Error"),
                FqName("kotlin.AssertionError"),
                FqName("kotlin.NotImplementedError")
            )
        )
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
                    members.addIfNotNull(exportFunction(declaration))

                is IrConstructor ->
                    members.addIfNotNull(exportConstructor(declaration))

                is IrProperty ->
                    members.addIfNotNull(exportProperty(declaration))

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
            .filter {
                it.classifierOrFail.isInterface &&
                        !it.isFunctionOrKFunction() &&
                        !it.isSuspendFunction() &&
                        !it.classifierOrFail.isClassWithFqName(FqNameUnsafe("kotlin.io.Serializable"))
            }.map { exportType(it) }

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
                                    klass.fqNameWhenAvailable!!.asString(),
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

    private fun IrDeclarationWithName.getExportedIdentifier(): String =
        with(name /*getJsNameOrKotlinName()*/) {
            if (isSpecial)
                error("Cannot export special name: ${name.asString()} for declaration $fqNameWhenAvailable")
            else identifier
        }

    private fun shouldDeclarationBeExported(declaration: IrDeclarationWithName): Boolean {

        if (declaration.fqNameWhenAvailable in context.additionalExportedDeclarations)
            return true

        if (declaration.isJsExport())
            return true

        if (declaration is IrSimpleFunction) {
            if (declaration.correspondingPropertySymbol?.owner?.isJsExport() == true)
                return true
        }

        return when (val parent = declaration.parent) {
            is IrDeclarationWithName -> shouldDeclarationBeExported(parent)
            is IrAnnotationContainer -> parent.isJsExport()
            else -> false
        }
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
        if (function.isMethodOfAny())
            return Exportability.NotNeeded
        if (function.name.asString().endsWith("-impl"))
            return Exportability.NotNeeded

        // TODO: Fix properties
        val name = function.getExportedIdentifier()
        if (name[0].isUpperCase()) return Exportability.Prohibited("Upper case function")
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
    get() =
        (owner as? IrClass)?.isInterface == true

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