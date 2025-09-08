/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.jsexport

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.lower.isEs6ConstructorReplacement
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.filterIsInstanceAnd
import org.jetbrains.kotlin.utils.keysToMap
import org.jetbrains.kotlin.utils.memoryOptimizedMapNotNull

class ExportModelGenerator(val context: JsIrBackendContext, val generateNamespacesForPackages: Boolean) {
    fun generateExport(file: IrPackageFragment): List<ExportedDeclaration> {
        val namespaceFqName = file.packageFqName
        val exports = file.declarations.memoryOptimizedMapNotNull { declaration ->
            declaration.takeIf { it.couldBeConvertedToExplicitExport() != true }?.let(::exportDeclaration)
        }

        return when {
            exports.isEmpty() -> emptyList()
            !generateNamespacesForPackages || namespaceFqName.isRoot -> exports
            else -> listOf(ExportedNamespace(namespaceFqName.toString(), exports))
        }
    }

    private fun exportDeclaration(declaration: IrDeclaration): ExportedDeclaration? {
        val candidate = getExportCandidate(declaration) ?: return null
        if (!shouldDeclarationBeExportedImplicitlyOrExplicitly(candidate, context, declaration)) return null

        return when (candidate) {
            is IrSimpleFunction -> exportFunction(candidate)
            is IrProperty -> exportProperty(candidate)
            is IrClass -> exportClass(candidate)
            is IrField -> null
            else -> irError("Can't export declaration") {
                withIrEntry("candidate", candidate)
            }
        }?.withAttributesFor(candidate)
    }

    private fun <T : ExportedDeclaration> T.withAttributesFor(declaration: IrDeclaration): T {
        if (declaration.isJsExportDefault()) {
            attributes.add(ExportedAttribute.DefaultExport)
        }

        return this
    }

    private fun exportClass(candidate: IrClass): ExportedDeclaration? {
        return if (candidate.isEnumClass) {
            exportEnumClass(candidate)
        } else {
            exportOrdinaryClass(candidate)
        }
    }

    private fun exportFunction(function: IrSimpleFunction): ExportedDeclaration? {
        return when (functionExportability(function)) {
            is Exportability.NotNeeded, is Exportability.Implicit, is Exportability.Prohibited -> null
            is Exportability.Allowed -> {
                ExportedFunction(
                    function.getExportedIdentifier(),
                    isStatic = function.isStaticMethod,
                    ir = function,
                )
            }
        }
    }

    private fun exportProperty(property: IrProperty): ExportedDeclaration? {
        for (accessor in listOfNotNull(property.getter, property.setter)) {
            // Frontend will report an error on an attempt to export an extension property.
            // Just to be safe, filter out such properties here as well.
            if (accessor.parameters.any { it.kind == IrParameterKind.ExtensionReceiver })
                return null
            if (accessor.isFakeOverride && !accessor.isAllowedFakeOverriddenDeclaration(context)) {
                return null
            }
        }

        return exportPropertyUnsafely(property)
    }

    private fun exportPropertyUnsafely(
        property: IrProperty,
    ): ExportedDeclaration = ExportedProperty(
        name = property.getExportedIdentifier(),
        irGetter = property.getter,
        irSetter = property.setter,
        isStatic = (property.getter ?: property.setter)?.isStaticMethodOfClass == true,
    )

    private fun exportEnumEntry(field: IrField): ExportedProperty {
        val irEnumEntry = field.correspondingEnumEntry
            ?: irError("Unable to find enum entry") {
                withIrEntry("field", field)
            }

        return ExportedProperty(
            name = irEnumEntry.getExportedIdentifier(),
            isStatic = true,
            irGetter = irEnumEntry.getInstanceFun
                ?: irError("Unable to find get instance fun") {
                    withIrEntry("field", field)
                },
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

        if (klass.isJsImplicitExport()) {
            return Exportability.Implicit
        }

        if (klass.isSingleFieldValueClass)
            return Exportability.Prohibited("Inline class ${klass.fqNameWhenAvailable}")

        return Exportability.Allowed
    }

    private fun exportDeclarationImplicitly(klass: IrClass): ExportedDeclaration {
        val name = klass.getExportedIdentifier()
        val (members, nestedClasses) = exportClassDeclarations(klass)
        return ExportedRegularClass(
            name = name,
            isInterface = true,
            members = members,
            nestedClasses = nestedClasses,
            ir = klass
        )
    }

    private fun exportOrdinaryClass(klass: IrClass): ExportedDeclaration? {
        when (val exportability = classExportability(klass)) {
            is Exportability.Prohibited -> irError(exportability.reason) {
                withIrEntry("klass", klass)
            }
            Exportability.NotNeeded -> return null
            Exportability.Implicit -> return exportDeclarationImplicitly(klass)
            Exportability.Allowed -> {}
        }

        val (members, nestedClasses) = exportClassDeclarations(klass)

        return exportClass(
            klass,
            members,
            nestedClasses
        )
    }

    private fun exportEnumClass(klass: IrClass): ExportedDeclaration? {
        when (val exportability = classExportability(klass)) {
            is Exportability.Prohibited -> irError(exportability.reason) {
                withIrEntry("klass", klass)
            }
            Exportability.NotNeeded -> return null
            Exportability.Implicit -> return exportDeclarationImplicitly(klass)
            Exportability.Allowed -> {}
        }

        val enumEntries = klass
            .declarations
            .filterIsInstance<IrField>()
            .mapNotNull { it.correspondingEnumEntry }

        enumEntries
            .keysToMap(enumEntries::indexOf)

        val (members, nestedClasses) = exportClassDeclarations(klass) { candidate ->
            val enumExportedMember = exportAsEnumMember(candidate)
            enumExportedMember
        }

        return exportClass(
            klass,
            members,
            nestedClasses
        )
    }

    private fun exportClassDeclarations(
        klass: IrClass,
        specialProcessing: (IrDeclarationWithName) -> ExportedDeclaration? = { null }
    ): ExportedClassDeclarationsInfo {
        val members = mutableListOf<ExportedDeclaration>()
        val specialMembers = mutableListOf<ExportedDeclaration>()
        val nestedClasses = mutableListOf<ExportedClass>()
        val isImplicitlyExportedClass = klass.isJsImplicitExport()

        for (declaration in klass.declarations) {
            val candidate = getExportCandidate(declaration) ?: continue
            if (isImplicitlyExportedClass && candidate !is IrClass) continue
            if (!shouldDeclarationBeExportedImplicitlyOrExplicitly(candidate, context, declaration)) continue
            if (candidate.isFakeOverride && klass.isInterface) continue

            val processingResult = specialProcessing(candidate)
            if (processingResult != null) {
                specialMembers.add(processingResult)
                continue
            }

            when (candidate) {
                is IrSimpleFunction ->
                    members.addIfNotNull(exportFunction(candidate))

                is IrConstructor -> continue

                is IrProperty ->
                    members.addIfNotNull(exportProperty(candidate))

                is IrClass -> {
                    if (klass.isInterface) {
                        nestedClasses.addIfNotNull(klass.companionObject()?.let { exportClass(it) as? ExportedClass })
                    } else {
                        val ec = exportClass(candidate)
                        if (ec is ExportedClass) {
                            nestedClasses.add(ec)
                        } else {
                            members.addIfNotNull(ec)
                        }
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

                else -> irError("Can't export member declaration") {
                    withIrEntry("declaration", declaration)
                }
            }
        }

        return ExportedClassDeclarationsInfo(
            specialMembers + members,
            nestedClasses
        )
    }

    private fun exportClass(
        klass: IrClass,
        members: List<ExportedDeclaration>,
        nestedClasses: List<ExportedClass>,
    ): ExportedDeclaration {
        val name = klass.getExportedIdentifier()

        return if (klass.kind == ClassKind.OBJECT) {
            return ExportedObject(
                ir = klass,
                name = name,
                members = members,
                nestedClasses = nestedClasses,
                irGetter = klass.objectGetInstanceFunction!!
            )
        } else {
            ExportedRegularClass(
                name = name,
                isInterface = klass.isInterface,
                members = members,
                nestedClasses = nestedClasses,
                ir = klass
            )
        }
    }

    private fun exportAsEnumMember(
        candidate: IrDeclarationWithName
    ): ExportedDeclaration? {
        return when (candidate) {
            is IrProperty -> {
                if (candidate.isAllowedFakeOverriddenDeclaration(context)) {
                    exportPropertyUnsafely(
                        candidate,
                    )
                } else null
            }

            is IrField -> {
                if (candidate.origin == IrDeclarationOrigin.FIELD_FOR_ENUM_ENTRY) {
                    exportEnumEntry(candidate)
                } else {
                    null
                }
            }

            else -> null
        }
    }

    private fun functionExportability(function: IrSimpleFunction): Exportability {
        if (function.isInline && function.typeParameters.any { it.isReified })
            return Exportability.Prohibited("Inline reified function")
        if (function.isSuspend)
            return Exportability.Prohibited("Suspend function")
        if (function.isFakeOverride && !function.isAllowedFakeOverriddenDeclaration(context))
            return Exportability.NotNeeded
        if (function.origin == JsLoweredDeclarationOrigin.BRIDGE_WITHOUT_STABLE_NAME ||
            function.origin == JsLoweredDeclarationOrigin.BRIDGE_PROPERTY_ACCESSOR ||
            function.origin == JsLoweredDeclarationOrigin.BRIDGE_WITH_STABLE_NAME ||
            function.origin == JsLoweredDeclarationOrigin.OBJECT_GET_INSTANCE_FUNCTION ||
            function.origin == JsLoweredDeclarationOrigin.JS_SHADOWED_EXPORT ||
            function.origin == JsLoweredDeclarationOrigin.ENUM_GET_INSTANCE_FUNCTION
        ) {
            return Exportability.NotNeeded
        }

        val parentClass = function.parent as? IrClass

        if (parentClass != null && parentClass.initEntryInstancesFun == function) {
            return Exportability.NotNeeded
        }

        val nameString = function.name.asString()
        if (nameString.endsWith("-impl"))
            return Exportability.NotNeeded


        // Workaround in case IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER is rewritten.
        // TODO: Remove this check KT-75095
        if (nameString.endsWith("\$") && function.parameters.any { "\$mask" in it.name.asString() }) {
            return Exportability.NotNeeded
        }

        val name = function.getExportedIdentifier()
        // TODO: Use [] syntax instead of prohibiting
        if (parentClass == null && name in allReservedWords)
            return Exportability.Prohibited("Name is a reserved word")

        return Exportability.Allowed
    }
}

private class ExportedClassDeclarationsInfo(
    val members: List<ExportedDeclaration>,
    val nestedClasses: List<ExportedClass>
) {
    operator fun component1() = members
    operator fun component2() = nestedClasses
}

private val IrFunction.isStaticMethod: Boolean
    get() = isEs6ConstructorReplacement || isStaticMethodOfClass

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

private fun shouldDeclarationBeExportedImplicitlyOrExplicitly(
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

    if (context.additionalExportedDeclarationNames.contains(declaration.fqNameWhenAvailable))
        return true

    if (context.additionalExportedDeclarations.contains(declaration))
        return true

    if (source is IrOverridableDeclaration<*>) {
        val overriddenNonEmpty = source.overriddenSymbols.isNotEmpty()

        if (overriddenNonEmpty) {
            return source.isOverriddenExported(context) ||
                    (source as? IrSimpleFunction)?.isMethodOfAny() == true // Handle names for special functions
                    || source.isAllowedFakeOverriddenDeclaration(context)
        }
    }

    if (declaration.isUnconditionallyExported())
        return true

    return when (val parent = declaration.parent) {
        is IrDeclarationWithName -> shouldDeclarationBeExported(parent, context)
        is IrAnnotationContainer -> parent.isUnconditionallyExported()
        else -> false
    }
}

fun IrOverridableDeclaration<*>.isAllowedFakeOverriddenDeclaration(context: JsIrBackendContext): Boolean {
    if (isOverriddenEnumProperty(context)) return true

    val firstExportedRealOverride = runIf(isFakeOverride) {
        resolveFakeOverrideMaybeAbstract { it === this || it.isFakeOverride || it.parentClassOrNull?.isExported(context) != true }
    } ?: return false

    return firstExportedRealOverride.parentClassOrNull.isExportedInterface(context) && !firstExportedRealOverride.isJsExportIgnore()
}

fun IrOverridableDeclaration<*>.isOverriddenEnumProperty(context: JsIrBackendContext) =
    overriddenSymbols
        .map { it.owner }
        .filterIsInstanceAnd<IrOverridableDeclaration<*>> {
            it.overriddenSymbols.isEmpty() && it.parentClassOrNull?.symbol == context.irBuiltIns.enumClass
        }
        .isNotEmpty()

fun IrOverridableDeclaration<*>.isOverriddenExported(context: JsIrBackendContext): Boolean =
    overriddenSymbols
        .any {
            val owner = it.owner as IrDeclarationWithName
            val candidate = getExportCandidate(owner) ?: owner
            shouldDeclarationBeExported(candidate, context, owner)
        }

fun IrDeclaration.isExported(context: JsIrBackendContext): Boolean {
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

fun IrDeclarationWithName.getExportedIdentifier(): String =
    with(getJsNameOrKotlinName()) {
        if (isSpecial)
            irError("Cannot export special name: ${name.asString()} for declaration") {
                withIrEntry("this", this@getExportedIdentifier)
            }
        else identifier
    }
