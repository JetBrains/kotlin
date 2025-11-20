/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ir

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.backend.js.initEntryInstancesFun
import org.jetbrains.kotlin.ir.backend.js.lower.coroutines.isPromisifiedWrapper
import org.jetbrains.kotlin.ir.backend.js.tsexport.Exportability
import org.jetbrains.kotlin.ir.backend.js.tsexport.ExportedVisibility
import org.jetbrains.kotlin.ir.backend.js.tsexport.toExportedVisibility
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.filterIsInstanceAnd

internal fun IrClass.exportability(): Exportability {
    when (kind) {
        ClassKind.ANNOTATION_CLASS ->
            return Exportability.Prohibited("Class ${fqNameWhenAvailable} with kind: ${kind}")

        ClassKind.OBJECT,
        ClassKind.CLASS,
        ClassKind.INTERFACE,
        ClassKind.ENUM_CLASS,
        ClassKind.ENUM_ENTRY -> {
        }
    }

    if (isJsImplicitExport()) {
        return Exportability.Implicit
    }

    if (isSingleFieldValueClass)
        return Exportability.Prohibited("Inline class ${fqNameWhenAvailable}")

    return Exportability.Allowed
}

internal fun IrSimpleFunction.exportability(context: JsIrBackendContext): Exportability {
    if (isInline && typeParameters.any { it.isReified })
        return Exportability.Prohibited("Inline reified function")
    if (isSuspend)
        return Exportability.Prohibited("Suspend function")
    if (isFakeOverride && !isAllowedFakeOverriddenDeclaration(context))
        return Exportability.NotNeeded
    if (origin == JsLoweredDeclarationOrigin.BRIDGE_WITHOUT_STABLE_NAME ||
        origin == JsLoweredDeclarationOrigin.BRIDGE_PROPERTY_ACCESSOR ||
        origin == JsLoweredDeclarationOrigin.BRIDGE_WITH_STABLE_NAME ||
        origin == JsLoweredDeclarationOrigin.OBJECT_GET_INSTANCE_FUNCTION ||
        origin == JsLoweredDeclarationOrigin.JS_SHADOWED_EXPORT ||
        origin == JsLoweredDeclarationOrigin.ENUM_GET_INSTANCE_FUNCTION
    ) {
        return Exportability.NotNeeded
    }

    val parentClass = parent as? IrClass

    if (parentClass != null && parentClass.initEntryInstancesFun == this) {
        return Exportability.NotNeeded
    }

    val nameString = name.asString()
    if (nameString.endsWith("-impl"))
        return Exportability.NotNeeded


    // Workaround in case IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER is rewritten.
    // TODO: Remove this check KT-75095
    if (nameString.endsWith("\$") && parameters.any { "\$mask" in it.name.asString() }) {
        return Exportability.NotNeeded
    }

    val name = getExportedIdentifier()
    // TODO: Use [] syntax instead of prohibiting
    if (parentClass == null && name in allReservedWords)
        return Exportability.Prohibited("Name is a reserved word")

    return Exportability.Allowed
}

internal fun getExportCandidate(declaration: IrDeclaration): IrDeclarationWithName? {
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

internal fun shouldDeclarationBeExportedImplicitlyOrExplicitly(
    declaration: IrDeclarationWithName,
    context: JsIrBackendContext,
    source: IrDeclaration = declaration
): Boolean {
    return declaration.isJsImplicitExport() || shouldDeclarationBeExported(declaration, context, source)
}

private fun shouldDeclarationBeExported(
    declaration: IrDeclarationWithName,
    context: JsIrBackendContext,
    source: IrDeclaration = declaration
): Boolean {
    // Formally, user have no ability to annotate EnumEntry as exported, without Enum Class
    // But, when we add @file:JsExport, the annotation appears on the all of enum entries
    // what make a wrong behaviour on non-exported members inside Enum Entry (check exportEnumClass and exportFileWithEnumClass tests)
    if (declaration is IrClass && declaration.kind == ClassKind.ENUM_ENTRY)
        return false

    if (declaration.isJsExportIgnore() || (declaration as? IrDeclarationWithVisibility)?.visibility?.isPublicAPI == false)
        return false

    if (declaration is IrField && declaration.isObjectInstanceField()) {
        // Object instance fields are generated with the public visibility because after InlineObjectsWithPureInitializationLowering
        // we replace calls to object getters with direct field accesses, which can be cross-module.
        // But those fields aren't meant to end up in DCE roots, which they would otherwise.
        return false
    }

    if (context.additionalExportedDeclarationNames.contains(declaration.fqNameWhenAvailable))
        return true

    if (context.additionalExportedDeclarations.contains(declaration))
        return true

    if (source is IrOverridableDeclaration<*>) {
        val overriddenNonEmpty = source.overriddenSymbols.isNotEmpty()

        if (overriddenNonEmpty) {
            return (source as? IrSimpleFunction)?.isMethodOfAny() == true // Handle names for special functions
                    || source.isAllowedFakeOverriddenDeclaration(context)
                    || source.isOverriddenExported(context)
        }
    }

    val parentModality = declaration.parentClassOrNull?.modality
    if (declaration is IrDeclarationWithVisibility
        && !(declaration is IrConstructor && declaration.isPrimary)
        && declaration.visibility == DescriptorVisibilities.PROTECTED
        && (parentModality == Modality.FINAL || parentModality == Modality.SEALED)
    ) {
        // Protected members inside final classes are effectively private.
        // Protected members inside sealed classes are effectively module-private.
        // The only exception is the primary constructor: we will set its visibility to private during
        // TypeScript export model generation, otherwise, if no (private) primary constructor is exported, there will be
        // a default constructor, which we don't want.
        return false
    }

    if (declaration.isExplicitlyExported())
        return true

    return when (val parent = declaration.parent) {
        is IrDeclarationWithName -> shouldDeclarationBeExported(parent, context)
        is IrAnnotationContainer -> parent.isExplicitlyExported()
        else -> false
    }
}

internal fun IrOverridableDeclaration<*>.isAllowedFakeOverriddenDeclaration(context: JsIrBackendContext): Boolean {
    if (isPromisifiedWrapper || isOverriddenEnumProperty(context)) return true

    val firstExportedRealOverride = runIf(isFakeOverride) {
        resolveFakeOverrideMaybeAbstract { it === this || it.isFakeOverride || it.parentClassOrNull?.isExported(context) != true }
    } ?: return false

    return firstExportedRealOverride.parentClassOrNull.isExportedInterface(context) && !firstExportedRealOverride.isJsExportIgnore()
}

internal fun IrOverridableDeclaration<*>.isOverriddenEnumProperty(context: JsIrBackendContext) =
    overriddenSymbols
        .map { it.owner }
        .filterIsInstanceAnd<IrOverridableDeclaration<*>> {
            it.overriddenSymbols.isEmpty() && it.parentClassOrNull?.symbol == context.irBuiltIns.enumClass
        }
        .isNotEmpty()

internal fun IrOverridableDeclaration<*>.isOverriddenExported(context: JsIrBackendContext): Boolean =
    overriddenSymbols
        .any {
            val owner = it.owner as IrDeclarationWithName
            val candidate = getExportCandidate(owner) ?: owner
            shouldDeclarationBeExported(candidate, context, owner)
        }

internal fun IrDeclaration.isExported(context: JsIrBackendContext): Boolean {
    val candidate = getExportCandidate(this) ?: return false
    return shouldDeclarationBeExported(candidate, context, this)
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

private val strictModeReservedWords = setOf(
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

internal val allReservedWords = reservedWords + strictModeReservedWords

fun IrDeclarationWithName.getExportedIdentifier(): String =
    with(getJsNameOrKotlinName()) {
        if (isSpecial)
            irError("Cannot export special name: ${name.asString()} for declaration") {
                withIrEntry("this", this@getExportedIdentifier)
            }
        else identifier
    }

internal val IrConstructor.exportedVisibility: ExportedVisibility
    get() = when (constructedClass.modality) {
        Modality.SEALED -> ExportedVisibility.PRIVATE
        Modality.FINAL if visibility == DescriptorVisibilities.PROTECTED -> ExportedVisibility.PRIVATE
        else -> visibility.toExportedVisibility()
    }