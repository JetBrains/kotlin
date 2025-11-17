/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.jsexport

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.correspondingEnumEntry
import org.jetbrains.kotlin.ir.backend.js.getInstanceFun
import org.jetbrains.kotlin.ir.backend.js.ir.*
import org.jetbrains.kotlin.ir.backend.js.lower.isEs6ConstructorReplacement
import org.jetbrains.kotlin.ir.backend.js.objectGetInstanceFunction
import org.jetbrains.kotlin.ir.backend.js.tsexport.Exportability
import org.jetbrains.kotlin.ir.backend.js.utils.couldBeConvertedToExplicitExport
import org.jetbrains.kotlin.ir.backend.js.utils.isJsExportDefault
import org.jetbrains.kotlin.ir.backend.js.utils.isJsImplicitExport
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.utils.addIfNotNull
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
        return when (function.exportability(context)) {
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
        when (val exportability = klass.exportability()) {
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
        when (val exportability = klass.exportability()) {
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
