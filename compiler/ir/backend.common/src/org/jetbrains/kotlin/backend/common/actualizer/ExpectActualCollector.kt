/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.FqName

internal class ExpectActualCollector(
    private val mainFragment: IrModuleFragment,
    private val dependentFragments: List<IrModuleFragment>,
    private val diagnosticsReporter: KtDiagnosticReporterWithImplicitIrBasedContext
) {
    fun collect(): Pair<Map<IrSymbol, IrSymbol>, Map<FqName, FqName>> {
        val result = mutableMapOf<IrSymbol, IrSymbol>()
        // Collect and link classes at first to make it possible to expand type aliases on the members linking
        val (actualMembers, expectActualTypeAliasMap) = result.appendExpectActualClassMap()
        result.appendExpectActualMemberMap(actualMembers, expectActualTypeAliasMap)
        return result to expectActualTypeAliasMap
    }

    private fun MutableMap<IrSymbol, IrSymbol>.appendExpectActualClassMap(): Pair<List<IrDeclarationBase>, Map<FqName, FqName>> {
        val actualClasses = mutableMapOf<String, IrClassSymbol>()
        // There is no list for builtins declarations; that's why they are being collected from typealiases
        val actualMembers = mutableListOf<IrDeclarationBase>()
        val expectActualTypeAliasMap = mutableMapOf<FqName, FqName>() // It's used to link members from expect class that have typealias actual

        val fragmentsWithActuals = dependentFragments.drop(1) + mainFragment
        val actualClassesAndMembersCollector = ActualClassesAndMembersCollector(actualClasses, actualMembers, expectActualTypeAliasMap)
        fragmentsWithActuals.forEach { actualClassesAndMembersCollector.collect(it) }

        val linkCollector = ClassLinksCollector(this, actualClasses, expectActualTypeAliasMap, diagnosticsReporter)
        dependentFragments.forEach { linkCollector.visitModuleFragment(it) }

        return actualMembers to expectActualTypeAliasMap
    }

    private fun MutableMap<IrSymbol, IrSymbol>.appendExpectActualMemberMap(
        actualMembers: List<IrDeclarationBase>,
        expectActualTypeAliasMap: Map<FqName, FqName>
    ) {
        val actualMembersMap = mutableMapOf<String, MutableList<IrDeclarationBase>>()
        for (actualMember in actualMembers) {
            actualMembersMap.getOrPut(generateIrElementFullNameFromExpect(actualMember, expectActualTypeAliasMap)) { mutableListOf() }
                .add(actualMember)
        }
        val collector = MemberLinksCollector(this, actualMembersMap, expectActualTypeAliasMap, diagnosticsReporter)
        dependentFragments.forEach { collector.visitModuleFragment(it) }
    }
}

private class ActualClassesAndMembersCollector(
    private val actualClasses: MutableMap<String, IrClassSymbol>,
    private val actualMembers: MutableList<IrDeclarationBase>,
    private val expectActualTypeAliasMap: MutableMap<FqName, FqName>
) {
    private val visitedActualClasses = mutableSetOf<IrClass>()

    fun collect(element: IrElement) {
        when (element) {
            is IrModuleFragment -> {
                for (file in element.files) {
                    collect(file)
                }
            }
            is IrTypeAlias -> {
                if (!element.isActual) return

                val expandedTypeSymbol = element.expandedType.classifierOrFail as IrClassSymbol
                addActualClass(element, expandedTypeSymbol)
                collect(expandedTypeSymbol.owner)

                expectActualTypeAliasMap[element.kotlinFqName] = expandedTypeSymbol.owner.kotlinFqName
            }
            is IrClass -> {
                if (element.isExpect || !visitedActualClasses.add(element)) return

                addActualClass(element, element.symbol)
                for (declaration in element.declarations) {
                    collect(declaration)
                }
            }
            is IrDeclarationContainer -> {
                for (declaration in element.declarations) {
                    collect(declaration)
                }
            }
            is IrEnumEntry -> {
                actualMembers.add(element) // If enum entry is located inside expect enum, then this code is not executed
            }
            is IrProperty -> {
                if (element.isExpect) return
                actualMembers.add(element)
            }
            is IrFunction -> {
                if (element.isExpect) return
                actualMembers.add(element)
            }
        }
    }

    private fun addActualClass(classOrTypeAlias: IrElement, classSymbol: IrClassSymbol) {
        actualClasses[generateActualIrClassOrTypeAliasFullName(classOrTypeAlias)] = classSymbol
    }
}

private class ClassLinksCollector(
    private val expectActualMap: MutableMap<IrSymbol, IrSymbol>,
    private val actualClasses: Map<String, IrClassSymbol>,
    private val expectActualTypeAliasMap: Map<FqName, FqName>,
    private val diagnosticsReporter: KtDiagnosticReporterWithImplicitIrBasedContext
) : IrElementVisitorVoid {
    override fun visitClass(declaration: IrClass) {
        if (!declaration.isExpect) return

        val actualClassSymbol = actualClasses[generateIrElementFullNameFromExpect(declaration, expectActualTypeAliasMap)]
        if (actualClassSymbol != null) {
            expectActualMap[declaration.symbol] = actualClassSymbol
            val actualClass = actualClassSymbol.owner
            for (expectTypeParameter in declaration.typeParameters) {
                actualClass.typeParameters.firstOrNull { it.name == expectTypeParameter.name }?.let { actualTypeParameter ->
                    expectActualMap[expectTypeParameter.symbol] = actualTypeParameter.symbol
                }
            }
        } else if (!declaration.containsOptionalExpectation()) {
            diagnosticsReporter.reportMissingActual(declaration)
        }

        visitElement(declaration)
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }
}

private class MemberLinksCollector(
    private val expectActualMap: MutableMap<IrSymbol, IrSymbol>,
    private val actualMembers: Map<String, List<IrDeclarationBase>>,
    private val typeAliasMap: Map<FqName, FqName>,
    private val diagnosticsReporter: KtDiagnosticReporterWithImplicitIrBasedContext
) : IrElementVisitorVoid {
    override fun visitFunction(declaration: IrFunction) {
        if (declaration.isExpect) addLink(declaration)
    }

    override fun visitProperty(declaration: IrProperty) {
        if (declaration.isExpect) addLink(declaration)
    }

    override fun visitEnumEntry(declaration: IrEnumEntry) {
        if ((declaration.parent as IrClass).isExpect) addLink(declaration)
    }

    private fun addLink(declaration: IrDeclarationBase) {
        val actualMemberMatches = actualMembers.getMatches(declaration, expectActualMap, typeAliasMap)
        when {
            actualMemberMatches.size == 1 -> {
                val actualMember = actualMemberMatches.single()
                expectActualMap[declaration.symbol] = actualMember.symbol
                if (declaration is IrProperty) {
                    val actualProperty = actualMember as IrProperty
                    declaration.getter!!.symbol.let { expectActualMap[it] = actualProperty.getter!!.symbol }
                    declaration.setter?.symbol?.let { expectActualMap[it] = actualProperty.setter!!.symbol }
                }
            }
            actualMemberMatches.size > 1 -> {
                // TODO: report AMBIGUOUS_ACTUALS here, see KT-57932
            }
            !declaration.parent.containsOptionalExpectation() && !(declaration is IrConstructor && declaration.isPrimary) -> {
                diagnosticsReporter.reportMissingActual(declaration)
            }
        }
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }
}