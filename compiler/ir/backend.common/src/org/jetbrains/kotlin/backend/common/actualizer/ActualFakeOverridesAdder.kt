/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.backend.common.CommonBackendErrors
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.overrides.IrOverrideChecker
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.OverridingUtil

/**
 * It adds fake overrides to non-expect classes inside common or multi-platform module,
 * taken from these non-expect classes actualized super classes.
 *
 * In case when a non-expect class has direct or indirect expect supertypes,
 * it may happen that the actual classes for these supertypes contain additional (non-actual) members that don't exist in their expect counterparts.
 * We still should have fake overrides generated for these members, but FIR2IR isn't able to see their base members in common or multi-platform module.
 * This class is intended to search for such situations and generate such fake overrides.
 */
internal class ActualFakeOverridesAdder(
    private val expectActualMap: Map<IrSymbol, IrSymbol>,
    private val expectToActualClassMap: Map<ClassId, IrClassSymbol>,
    private val diagnosticsReporter: KtDiagnosticReporterWithImplicitIrBasedContext,
    private val typeSystemContext: IrTypeSystemContext
) : IrElementVisitorVoid {
    private val overrideChecker = IrOverrideChecker(typeSystemContext, emptyList())
    private val missingActualMembersMap = mutableMapOf<IrClass, FakeOverrideInfo>()

    override fun visitClass(declaration: IrClass) {
        extractMissingActualMembersFromSupertypes(declaration)
        visitElement(declaration)
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    private fun extractMissingActualMembersFromSupertypes(klass: IrClass): FakeOverrideInfo {
        missingActualMembersMap[klass]?.let { return it }

        val missingActualMembers = FakeOverrideInfo()
        missingActualMembersMap[klass] = missingActualMembers

        // New members from supertypes are only relevant for not expect (ordinary) classes
        // New members from the current class are only relevant for actualized expect classes

        val processedMembers = FakeOverrideInfo()
        for (superType in klass.superTypes) {
            val membersFromSupertype = extractMissingActualMembersFromSupertypes(superType.classifierOrFail.owner as IrClass)
            if (!klass.isExpect) {
                appendMissingMembersToNotExpectClass(missingActualMembers, klass, membersFromSupertype.allSymbols(), processedMembers)
            }
        }

        val actualClass = expectActualMap[klass.symbol]?.owner as? IrClass ?: return missingActualMembers

        missingActualMembers.appendMissingMembersFromActualizedExpectClass(klass, actualClass)

        return missingActualMembers
    }

    private fun appendMissingMembersToNotExpectClass(
        fakeOverrideInfo: FakeOverrideInfo,
        klass: IrClass,
        membersFromSupertype: List<IrSymbol>,
        processedMembers: FakeOverrideInfo
    ) {
        for (symbolFromSupertype in membersFromSupertype) {
            val memberFromSupertype = symbolFromSupertype.owner as IrDeclaration

            if (memberFromSupertype is IrOverridableMember) {
                // We can land here because of a hierarchy like
                // actual A -> common B -> actual C
                // where C defines a member x and A overrides the member x.
                // We will first add a fake-override x to B and then land here.
                // In this case we don't want to add a fake-override on top of the real override to A.
                // Instead, we add the fake-override x to the overridden symbols of A.x.

                @Suppress("UNCHECKED_CAST")
                val override = klass.declarations.firstOrNull {
                    it is IrOverridableMember &&
                            overrideChecker.isOverridableBy(
                                superMember = memberFromSupertype,
                                subMember = it,
                                checkIsInlineFlag = false,
                            ).result == OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE
                } as? IrOverridableDeclaration<IrSymbol>

                if (override != null) {
                    override.overriddenSymbols += symbolFromSupertype
                    continue
                }
            }

            val newMember = createFakeOverrideMember(listOf(memberFromSupertype), klass)
            val matchingFakeOverrides = collectActualCallablesMatchingToSpecificExpect(
                newMember.symbol,
                fakeOverrideInfo.getMembersForActual(newMember),
                expectToActualClassMap,
                typeSystemContext
            )
            if (matchingFakeOverrides.isEmpty()) {
                processedMembers.addMember(memberFromSupertype as IrOverridableDeclaration<*>)
                fakeOverrideInfo.addMember(newMember)
                klass.addMember(newMember)
            } else {
                val baseMembers = collectActualCallablesMatchingToSpecificExpect(
                    newMember.symbol,
                    processedMembers.getMembersForActual(newMember),
                    expectToActualClassMap,
                    typeSystemContext
                )

                val errorFactory =
                    if (baseMembers.all { ((it.owner as IrDeclaration).parent as IrClass).isInterface } && (memberFromSupertype.parent as IrClass).isInterface)
                        CommonBackendErrors.MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED
                    else
                        CommonBackendErrors.MANY_IMPL_MEMBER_NOT_IMPLEMENTED
                diagnosticsReporter.at(klass).report(
                    errorFactory,
                    klass.name.asString(),
                    (memberFromSupertype as IrDeclarationWithName).name.asString()
                )
            }
        }
    }

    private fun FakeOverrideInfo.appendMissingMembersFromActualizedExpectClass(
        expectClass: IrClass,
        actualClass: IrClass,
    ) {
        val actualWithCorrespondingExpectMembers = hashSetOf<IrSymbol>().apply {
            expectClass.declarations.mapNotNullTo(this) { expectActualMap[(it as? IrOverridableDeclaration<*>)?.symbol] }
        }

        for (actualMember in actualClass.declarations) {
            if (actualMember is IrOverridableDeclaration<*> &&
                (actualMember as? IrDeclarationWithVisibility)?.visibility != DescriptorVisibilities.PRIVATE &&
                !actualWithCorrespondingExpectMembers.contains(actualMember.symbol)
            ) {
                addMember(actualMember)
            }
        }
    }

    private class FakeOverrideInfo {
        val functionsByName: MutableMap<Name, MutableList<IrSymbol>> = mutableMapOf()
        val propertiesByName: MutableMap<Name, MutableList<IrSymbol>> = mutableMapOf()

        fun allSymbols(): List<IrSymbol> {
            return buildList {
                functionsByName.values.flatMapTo(this) { it }
                propertiesByName.values.flatMapTo(this) { it }
            }
        }

        private fun getCorrespondingMap(member: IrOverridableDeclaration<*>): MutableMap<Name, MutableList<IrSymbol>> {
            return when (member) {
                is IrFunction -> functionsByName
                is IrProperty -> propertiesByName
                else -> error("Unsupported declaration type: $member")
            }
        }

        fun addMember(member: IrOverridableDeclaration<*>) {
            getCorrespondingMap(member).getOrPut(member.name) { mutableListOf() } += member.symbol
        }

        fun getMembersForActual(actualMember: IrOverridableDeclaration<*>): List<IrSymbol> {
            return getCorrespondingMap(actualMember)[actualMember.name].orEmpty()
        }
    }
}
