/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.PsiIrFileEntry
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeAliasSymbol
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.mpp.DeclarationSymbolMarker
import org.jetbrains.kotlin.mpp.RegularClassSymbolMarker
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.calls.mpp.AbstractExpectActualChecker
import org.jetbrains.kotlin.resolve.calls.mpp.AbstractExpectActualMatcher
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCheckingCompatibility
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility
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
    private val diagnosticsReporter: IrDiagnosticReporter,
    private val expectActualTracker: ExpectActualTracker?,
    private val extraActualDeclarationExtractors: List<IrExtraActualDeclarationExtractor>,
) {
    fun collectClassActualizationInfo(): ClassActualizationInfo {
        val expectTopLevelDeclarations = ExpectTopLevelDeclarationCollector.collect(dependentFragments)
        val fragmentsWithActuals = dependentFragments.drop(1) + mainFragment
        return ActualDeclarationsCollector.collectActuals(
            fragmentsWithActuals, expectTopLevelDeclarations, extraActualDeclarationExtractors
        )
    }

    fun matchAllExpectDeclarations(classActualizationInfo: ClassActualizationInfo): IrExpectActualMap {
        val linkCollector = ExpectActualLinkCollector()
        val linkCollectorContext = ExpectActualLinkCollector.MatchingContext(
            typeSystemContext, diagnosticsReporter, expectActualTracker, classActualizationInfo, null
        )
        dependentFragments.forEach { linkCollector.collectAndCheckMapping(it, linkCollectorContext) }
        // It doesn't make sense to link expects from the last module because actuals always should be located in another module
        // Thus relevant actuals are always missing for the last module
        // But the collector should be run anyway to detect and report "hanging" expect declarations
        linkCollector.collectAndCheckMapping(mainFragment, linkCollectorContext)
        return linkCollectorContext.expectActualMap
    }
}

data class ClassActualizationInfo(
    // mapping from classId of actual class/typealias to itself/typealias expansion
    val actualClasses: ActualClassMapping,
    // mapping from classId to actual typealias
    val actualTypeAliases: Map<ClassId, IrTypeAliasSymbol>,
    val actualTopLevels: Map<CallableId, List<IrSymbol>>,
    val actualSymbolsToFile: Map<IrSymbol, IrFile?>,
) {
    fun getActualWithoutExpansion(classId: ClassId): IrSymbol? {
        return actualTypeAliases[classId] ?: actualClasses[classId]
    }

    class ActualClassMapping(private val actualClasses: Map<ClassId, IrClassSymbol>) {
        /*
         * expect class may be actualized to another expect class via actual typealias, so
         *   we need to actualize them recursively until there will be a non-expect class
         */
        operator fun get(classId: ClassId?): IrClassSymbol? {
            val actualized = actualClasses[classId] ?: return null
            if (actualized.owner.isExpect) {
                return get(actualized.owner.classIdOrFail)
            }
            return actualized
        }
    }
}

private class ExpectTopLevelDeclarations(val classes: Map<ClassId, IrClassSymbol>, val callables: Map<CallableId, List<IrSymbol>>)

private class ExpectTopLevelDeclarationCollector {
    companion object {
        fun collect(fragments: List<IrModuleFragment>): ExpectTopLevelDeclarations {
            val collector = ExpectTopLevelDeclarationCollector()
            collector.collect(fragments)
            return ExpectTopLevelDeclarations(collector.expectTopLevelClasses, collector.expectTopLevelCallables)
        }
    }

    private val expectTopLevelClasses = mutableMapOf<ClassId, IrClassSymbol>()
    private val expectTopLevelCallables = mutableMapOf<CallableId, MutableList<IrSymbol>>()

    fun collect(fragments: List<IrModuleFragment>) {
        for (fragment in fragments) {
            for (file in fragment.files) {
                for (declaration in file.declarations) {
                    if (declaration.isExpect && declaration.isTopLevel) {
                        fun addCallable(callableId: CallableId) {
                            val list = expectTopLevelCallables.getOrPut(callableId) { mutableListOf() }
                            list.add(declaration.symbol)
                        }

                        when (declaration) {
                            is IrClass -> expectTopLevelClasses[declaration.classIdOrFail] = declaration.symbol
                            is IrProperty -> addCallable(declaration.callableId)
                            is IrFunction -> addCallable(declaration.callableId)
                        }
                    }
                }
            }
        }
    }
}

private class ActualDeclarationsCollector(private val expectTopLevelDeclarations: ExpectTopLevelDeclarations) {
    companion object {
        fun collectActuals(
            fragments: List<IrModuleFragment>,
            expectTopLevelDeclarations: ExpectTopLevelDeclarations,
            extraActualDeclarationExtractors: List<IrExtraActualDeclarationExtractor>,
        ): ClassActualizationInfo {
            val collector = ActualDeclarationsCollector(expectTopLevelDeclarations)
            for (fragment in fragments) {
                collector.collect(fragment)
            }
            for (extractor in extraActualDeclarationExtractors) {
                collector.collectExtraActualDeclarations(extractor)
            }
            return ClassActualizationInfo(
                ClassActualizationInfo.ActualClassMapping(collector.actualClasses),
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
                recordMappingsForNestedClassesActualizedViaTypealias(classId, expandedTypeSymbol)
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

    private fun collectExtraActualDeclarations(extraActualDeclarationExtractor: IrExtraActualDeclarationExtractor) {
        for (classSymbol in expectTopLevelDeclarations.classes.values) {
            collectExtraActualClasses(extraActualDeclarationExtractor, classSymbol.owner)
        }
        for ((callableId, callableSymbols) in expectTopLevelDeclarations.callables) {
            val expectTopLevelCallables = callableSymbols.mapNotNull {
                when (val owner = it.owner) {
                    is IrProperty -> owner
                    is IrFunction -> owner
                    else -> null
                }
            }
            collectExtraActualCallables(extraActualDeclarationExtractor, expectTopLevelCallables, callableId)
        }
    }

    private fun collectExtraActualClasses(extraActualDeclarationExtractor: IrExtraActualDeclarationExtractor, expectClass: IrClass) {
        val actualClassSymbol = extraActualDeclarationExtractor.extract(expectClass) ?: return
        val classId = expectClass.classIdOrFail
        if (actualClasses.containsKey(classId)) return // TODO: report actual classes collision, KT-67740

        actualClasses[classId] = actualClassSymbol

        for (declaration in expectClass.declarations) {
            if (declaration is IrClass) {
                collectExtraActualClasses(extraActualDeclarationExtractor, declaration)
            }
        }
    }

    private fun collectExtraActualCallables(
        extraActualDeclarationExtractor: IrExtraActualDeclarationExtractor,
        expectTopLevelCallables: List<IrDeclarationWithName>,
        callableId: CallableId
    ) {
        for (actualCallableSymbol in extraActualDeclarationExtractor.extract(expectTopLevelCallables, callableId)) {
            recordActualCallable(actualCallableSymbol.owner as IrDeclarationWithName, callableId, writeActualSymbolToFile = false)
        }
    }

    /**
     * For given actual typealias goes through all nested classes in expect class to record their mappings in [actualClasses].
     *
     * This is needed because in case of `expect` nested classes actualized via typealias we can't simply find actual symbol by
     * `expect` `ClassId` (like we do for top-level classes), because `ClassId` is different.
     * For example, `expect` class `com/example/ExpectClass.Nested` may have actual with id `real/package/ActualTypeliasTarget.Nested`.
     */
    private fun recordMappingsForNestedClassesActualizedViaTypealias(typealiasClassId: ClassId, actualClassSymbol: IrClassSymbol) {
        fun recordRecursively(expectClass: IrClass, actualClass: IrClass) {
            val actualNestedClassesByName = actualClass.nestedClasses.associateBy { it.name }

            for (expectNestedClass in expectClass.nestedClasses) {
                val actualNestedClass = actualNestedClassesByName[expectNestedClass.name] ?: continue
                actualClasses[expectNestedClass.classIdOrFail] = actualNestedClass.symbol
                recordRecursively(expectNestedClass, actualNestedClass)
            }
        }

        val expectClassSymbol = expectTopLevelDeclarations.classes[typealiasClassId] ?: return
        recordRecursively(expectClassSymbol.owner, actualClassSymbol.owner)
    }

    private fun recordActualCallable(
        callableDeclaration: IrDeclarationWithName,
        callableId: CallableId,
        writeActualSymbolToFile: Boolean = true,
    ) {
        if (callableId.classId == null) {
            actualTopLevels
                .getOrPut(callableId) { mutableListOf() }
                .add(callableDeclaration.symbol)
            if (writeActualSymbolToFile) {
                actualSymbolsToFile[callableDeclaration.symbol] = currentFile
            }
        }
    }
}

private class ExpectActualLinkCollector {
    fun collectAndCheckMapping(declaration: IrModuleFragment, data: MatchingContext) {
        ExpectActualLinkCollectorVisitor.visitModuleFragment(declaration, data)
    }

    private object ExpectActualLinkCollectorVisitor : IrElementVisitor<Unit, MatchingContext> {
        override fun visitFile(declaration: IrFile, data: MatchingContext) {
            super.visitFile(declaration, data.withNewCurrentFile(declaration))
        }

        override fun visitFunction(declaration: IrFunction, data: MatchingContext) {
            if (declaration.isExpect) {
                // The function is top level because the visitor doesn't visit function/class children recursively
                matchExpectTopLevelCallable(declaration, declaration.callableId, data)
            }
        }

        override fun visitProperty(declaration: IrProperty, data: MatchingContext) {
            if (declaration.isExpect) {
                // The property is top level because the visitor doesn't visit function/class children recursively
                matchExpectTopLevelCallable(declaration, declaration.callableId, data)
            }
        }

        private fun matchExpectTopLevelCallable(declaration: IrDeclarationWithName, callableId: CallableId, context: MatchingContext) {
            matchAndCheckExpectTopLevelDeclaration(
                declaration.symbol,
                context.classActualizationInfo.actualTopLevels[callableId].orEmpty(),
                context,
            )
        }

        override fun visitClass(declaration: IrClass, data: MatchingContext) {
            if (!declaration.isExpect) return
            val classId = declaration.classIdOrFail
            val expectClassSymbol = declaration.symbol
            val actualClassLikeSymbol = data.classActualizationInfo.getActualWithoutExpansion(classId)
            matchAndCheckExpectTopLevelDeclaration(expectClassSymbol, listOfNotNull(actualClassLikeSymbol), data)
        }

        private fun matchAndCheckExpectTopLevelDeclaration(
            expectSymbol: IrSymbol,
            actualSymbols: List<IrSymbol>,
            context: MatchingContext,
        ) {
            val matched = AbstractExpectActualMatcher.matchSingleExpectTopLevelDeclarationAgainstPotentialActuals(
                expectSymbol,
                actualSymbols,
                context,
            )
            if (matched != null) {
                AbstractExpectActualChecker.checkSingleExpectTopLevelDeclarationAgainstMatchedActual(
                    expectSymbol,
                    matched,
                    context,
                    context.languageVersionSettings,
                )
            }
        }

        override fun visitElement(element: IrElement, data: MatchingContext) {
            element.acceptChildren(this, data)
        }
    }

    class MatchingContext(
        typeSystemContext: IrTypeSystemContext,
        private val diagnosticsReporter: IrDiagnosticReporter,
        private val expectActualTracker: ExpectActualTracker?,
        internal val classActualizationInfo: ClassActualizationInfo,
        private val currentExpectFile: IrFile?,
        val expectActualMap: IrExpectActualMap = IrExpectActualMap(),
    ) : IrExpectActualMatchingContext(typeSystemContext, classActualizationInfo.actualClasses) {

        private val currentExpectIoFile by lazy(LazyThreadSafetyMode.PUBLICATION) { currentExpectFile?.toIoFile() }

        internal val languageVersionSettings: LanguageVersionSettings get() = diagnosticsReporter.languageVersionSettings

        fun withNewCurrentFile(newCurrentFile: IrFile) =
            MatchingContext(
                typeContext, diagnosticsReporter, expectActualTracker, classActualizationInfo, newCurrentFile, expectActualMap
            )

        override fun onMatchedDeclarations(expectSymbol: IrSymbol, actualSymbol: IrSymbol) {
            expectActualTracker?.reportWithCurrentFile(actualSymbol)
            recordActualForExpectDeclaration(expectSymbol, actualSymbol, expectActualMap, diagnosticsReporter)
        }

        override fun onIncompatibleMembersFromClassScope(
            expectSymbol: DeclarationSymbolMarker,
            actualSymbolsByIncompatibility: Map<ExpectActualCheckingCompatibility.Incompatible<*>, List<DeclarationSymbolMarker>>,
            containingExpectClassSymbol: RegularClassSymbolMarker?,
            containingActualClassSymbol: RegularClassSymbolMarker?
        ) {
            require(expectSymbol is IrSymbol)
            for ((incompatibility, actualMemberSymbols) in actualSymbolsByIncompatibility) {
                for (actualSymbol in actualMemberSymbols) {
                    require(actualSymbol is IrSymbol)
                    diagnosticsReporter.reportExpectActualIncompatibility(expectSymbol, actualSymbol, incompatibility)
                }
            }
        }

        override fun onMismatchedMembersFromClassScope(
            expectSymbol: DeclarationSymbolMarker,
            actualSymbolsByIncompatibility: Map<ExpectActualMatchingCompatibility.Mismatch, List<DeclarationSymbolMarker>>,
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
                    diagnosticsReporter.reportExpectActualMismatch(expectSymbol, actualSymbol, incompatibility)
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
