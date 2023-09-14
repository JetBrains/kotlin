/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.PsiIrFileEntry
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.callableId
import org.jetbrains.kotlin.ir.util.classIdOrFail
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.mpp.DeclarationSymbolMarker
import org.jetbrains.kotlin.mpp.RegularClassSymbolMarker
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.calls.mpp.AbstractExpectActualCompatibilityChecker
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility
import java.io.File

/**
 * This class is used to collect mapping between all expect and actuals declarations, which are declared in passed module fragments
 * It does not process any declarations which may appear after actualization of supertypes (like fake-overrides in classes with
 *   expect supertypes)
 *
 * The main method of this class, `collect` returns a pair of two values:
 * - `expectToActualMap` is the main storage of all mapped declarations. Key is symbol of expect declaration and the value is symbol of
 *     corresponding actual declaration
 * - `classActualizationInfo` is a storage which keeps information about types (class) actualization, which can be used later for type
 *     refinement if needed
 *
 * If some declarations didn't match (or there was missing/actual) then corresponding declarations won't be stored in `expectToActualMap`.
 *   Instead of that an error will be reported to `diagnosticReporter`
 */
internal class ExpectActualCollector(
    private val mainFragment: IrModuleFragment,
    private val dependentFragments: List<IrModuleFragment>,
    private val typeSystemContext: IrTypeSystemContext,
    private val diagnosticsReporter: KtDiagnosticReporterWithImplicitIrBasedContext,
    private val expectActualTracker: ExpectActualTracker?,
) {
    data class Result(val expectToActualMap: MutableMap<IrSymbol, IrSymbol>, val classActualizationInfo: ClassActualizationInfo)

    fun collect(): Result {
        val result = mutableMapOf<IrSymbol, IrSymbol>()
        // Collect and link classes at first to make it possible to expand type aliases on the members linking
        val actualDeclarations = collectActualDeclarations()
        matchAllExpectDeclarations(result, actualDeclarations)
        return Result(result, actualDeclarations)
    }

    private fun collectActualDeclarations(): ClassActualizationInfo {
        val fragmentsWithActuals = dependentFragments.drop(1) + mainFragment
        return ActualDeclarationsCollector.collectActualsFromFragments(fragmentsWithActuals)
    }

    private fun matchAllExpectDeclarations(
        destination: MutableMap<IrSymbol, IrSymbol>,
        classActualizationInfo: ClassActualizationInfo,
    ) {
        val linkCollector = ExpectActualLinkCollector()
        val linkCollectorContext =
            ExpectActualLinkCollector.MatchingContext(
                typeSystemContext, destination, diagnosticsReporter, expectActualTracker, classActualizationInfo, null
            )
        dependentFragments.forEach { linkCollector.visitModuleFragment(it, linkCollectorContext) }
        // It doesn't make sense to link expects from the last module because actuals always should be located in another module
        // Thus relevant actuals are always missing for the last module
        // But the collector should be run anyway to detect and report "hanging" expect declarations
        linkCollector.visitModuleFragment(mainFragment, linkCollectorContext)
    }
}

internal data class ClassActualizationInfo(
    // mapping from classId of actual class/typealias to itself/typealias expansion
    val actualClasses: Map<ClassId, IrClassSymbol>,
    // mapping from classId to actual typealias
    val actualTypeAliases: Map<ClassId, IrTypeAliasSymbol>,
    val actualTopLevels: Map<CallableId, List<IrSymbol>>,
    val actualSymbolsToFile: Map<IrSymbol, IrFile?>,
) {
    fun getActualWithoutExpansion(classId: ClassId): IrSymbol? {
        return actualTypeAliases[classId] ?: actualClasses[classId]
    }
}



private class ActualDeclarationsCollector {
    companion object {
        fun collectActualsFromFragments(fragments: List<IrModuleFragment>): ClassActualizationInfo {
            val collector = ActualDeclarationsCollector()
            for (fragment in fragments) {
                collector.collect(fragment)
            }
            return ClassActualizationInfo(
                collector.actualClasses,
                collector.actualTypeAliasesWithoutExpansion,
                collector.actualTopLevels,
                collector.actualSymbolsToFile
            )
        }
    }

    private val actualClasses: MutableMap<ClassId, IrClassSymbol> = mutableMapOf()
    private val actualTypeAliasesWithoutExpansion: MutableMap<ClassId, IrTypeAliasSymbol> = mutableMapOf()
    private val actualTopLevels: MutableMap<CallableId, MutableList<IrSymbol>> = mutableMapOf()
    private val actualSymbolsToFile: MutableMap<IrSymbol, IrFile?> = mutableMapOf()

    private val visitedActualClasses = mutableSetOf<IrClass>()
    private var currentFile: IrFile? = null

    private fun collect(element: IrElement) {
        when (element) {
            is IrModuleFragment -> {
                for (file in element.files) {
                    collect(file)
                }
            }
            is IrFile -> {
                currentFile = element
                for (declaration in element.declarations) {
                    collect(declaration)
                }
            }
            is IrTypeAlias -> {
                if (!element.isActual) return

                val classId = element.classIdOrFail
                val expandedTypeSymbol = element.expandedType.classifierOrFail as IrClassSymbol

                actualClasses[classId] = expandedTypeSymbol
                actualTypeAliasesWithoutExpansion[classId] = element.symbol
                actualSymbolsToFile[expandedTypeSymbol] = currentFile
                actualSymbolsToFile[element.symbol] = currentFile

                collect(expandedTypeSymbol.owner)
            }
            is IrClass -> {
                if (element.isExpect || !visitedActualClasses.add(element)) return

                actualClasses[element.classIdOrFail] = element.symbol
                actualSymbolsToFile[element.symbol] = currentFile
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
                recordActualCallable(element, element.callableId) // If enum entry is located inside expect enum, then this code is not executed
            }
            is IrProperty -> {
                if (element.isExpect) return
                recordActualCallable(element, element.callableId)
            }
            is IrFunction -> {
                if (element.isExpect) return
                recordActualCallable(element, element.callableId)
            }
        }
    }

    private fun recordActualCallable(callableDeclaration: IrDeclarationWithName, callableId: CallableId) {
        if (callableId.classId == null) {
            actualTopLevels
                .getOrPut(callableId) { mutableListOf() }
                .add(callableDeclaration.symbol)
            actualSymbolsToFile[callableDeclaration.symbol] = currentFile
        }
    }
}

private class ExpectActualLinkCollector : IrElementVisitor<Unit, ExpectActualLinkCollector.MatchingContext> {

    override fun visitFile(declaration: IrFile, data: MatchingContext) {
        super.visitFile(declaration, data.withNewCurrentFile(declaration))
    }

    override fun visitFunction(declaration: IrFunction, data: MatchingContext) {
        if (declaration.isExpect) {
            matchExpectCallable(declaration, declaration.callableId, data)
        }
    }

    override fun visitProperty(declaration: IrProperty, data: MatchingContext) {
        if (declaration.isExpect) {
            matchExpectCallable(declaration, declaration.callableId, data)
        }
    }

    private fun matchExpectCallable(declaration: IrDeclarationWithName, callableId: CallableId, context: MatchingContext) {
        matchExpectDeclaration(
            declaration.symbol,
            context.classActualizationInfo.actualTopLevels[callableId].orEmpty(),
            context
        )
    }

    override fun visitClass(declaration: IrClass, data: MatchingContext) {
        if (!declaration.isExpect) return
        val classId = declaration.classIdOrFail
        val expectClassSymbol = declaration.symbol
        val actualClassLikeSymbol = data.classActualizationInfo.getActualWithoutExpansion(classId)
        matchExpectDeclaration(expectClassSymbol, listOfNotNull(actualClassLikeSymbol), data)
    }

    private fun matchExpectDeclaration(
        expectSymbol: IrSymbol,
        actualSymbols: List<IrSymbol>,
        context: MatchingContext
    ) {
        AbstractExpectActualCompatibilityChecker.matchSingleExpectTopLevelDeclarationAgainstPotentialActuals(
            expectSymbol,
            actualSymbols,
            context,
        )
    }

    override fun visitElement(element: IrElement, data: MatchingContext) {
        element.acceptChildren(this, data)
    }

    class MatchingContext(
        typeSystemContext: IrTypeSystemContext,
        private val destination: MutableMap<IrSymbol, IrSymbol>,
        private val diagnosticsReporter: KtDiagnosticReporterWithImplicitIrBasedContext,
        private val expectActualTracker: ExpectActualTracker?,
        internal val classActualizationInfo: ClassActualizationInfo,
        private val currentExpectFile: IrFile?,
    ) : IrExpectActualMatchingContext(typeSystemContext, classActualizationInfo.actualClasses) {

        private val currentExpectIoFile by lazy(LazyThreadSafetyMode.PUBLICATION) { currentExpectFile?.toIoFile() }

        fun withNewCurrentFile(newCurrentFile: IrFile) =
            MatchingContext(
                typeContext, destination, diagnosticsReporter, expectActualTracker, classActualizationInfo, newCurrentFile
            )

        override fun onMatchedClasses(expectClassSymbol: IrClassSymbol, actualClassSymbol: IrClassSymbol) {
            destination[expectClassSymbol] = actualClassSymbol
            expectActualTracker?.reportWithCurrentFile(actualClassSymbol)
            recordActualForExpectDeclaration(expectClassSymbol, actualClassSymbol, destination)
        }

        override fun onMatchedCallables(expectSymbol: IrSymbol, actualSymbol: IrSymbol) {
            expectActualTracker?.reportWithCurrentFile(actualSymbol)
            recordActualForExpectDeclaration(expectSymbol, actualSymbol, destination)
        }

        override fun onMismatchedMembersFromClassScope(
            expectSymbol: DeclarationSymbolMarker,
            actualSymbolsByIncompatibility: Map<ExpectActualCompatibility.Incompatible<*>, List<DeclarationSymbolMarker>>,
            containingExpectClassSymbol: RegularClassSymbolMarker?,
            containingActualClassSymbol: RegularClassSymbolMarker?,
        ) {
            require(expectSymbol is IrSymbol)
            if (actualSymbolsByIncompatibility.isEmpty() && !expectSymbol.owner.containsOptionalExpectation()) {
                diagnosticsReporter.reportMissingActual(expectSymbol)
            }
            for ((incompatibility, actualMemberSymbols) in actualSymbolsByIncompatibility) {
                for (actualSymbol in actualMemberSymbols) {
                    require(actualSymbol is IrSymbol)
                    diagnosticsReporter.reportIncompatibleExpectActual(expectSymbol, actualSymbol, incompatibility)
                }
            }
        }

        private fun ExpectActualTracker.reportWithCurrentFile(actualSymbol: IrSymbol) {
            if (currentExpectFile != null) {
                val actualIoFile = classActualizationInfo.actualSymbolsToFile[actualSymbol]?.toIoFile()
                if (actualIoFile != null) {
                    report(currentExpectIoFile!!, actualIoFile)
                }
            }
        }
    }
}

private fun IrFile.toIoFile(): File? = when (val fe = fileEntry) {
    is PsiIrFileEntry -> fe.psiFile.virtualFile.let { it.fileSystem.getNioPath(it)?.toFile() }
    else -> File(fileEntry.name).takeIf { it.exists() }
}