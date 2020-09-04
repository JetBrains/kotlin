package org.jetbrains.konan.resolve.konan

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.jetbrains.cidr.lang.preprocessor.OCInclusionContext
import com.jetbrains.cidr.lang.preprocessor.OCInclusionContextUtil
import com.jetbrains.cidr.lang.preprocessor.OCModuleResolver
import com.jetbrains.cidr.lang.symbols.OCSymbol
import com.jetbrains.cidr.lang.symbols.cpp.OCIncludeSymbol
import com.jetbrains.cidr.lang.symbols.cpp.OCMacroSymbol
import com.jetbrains.cidr.lang.symbols.cpp.OCUndefMacroSymbol
import com.jetbrains.cidr.lang.symbols.objc.OCModuleImportSymbol
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTable
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTablesCache
import com.jetbrains.cidr.lang.symbols.symtable.SymbolTableProvider
import com.jetbrains.cidr.util.events.CidrEventSpan
import org.jetbrains.konan.KonanOCSwiftBundle
import org.jetbrains.konan.resolve.KtDependencyGraph
import org.jetbrains.konan.resolve.translation.KtFileTranslator

object KonanBridgeSymbolLoading {
    private const val traceCategory: String = "konan"
    private const val traceTitle: String = "processKonan"
    private val indicatorText: String
        get() = KonanOCSwiftBundle.message("processing.konan")

    private var lastRequestNo: Int = 0
    private var indicator: ProgressIndicator? = null

    fun runWhenSmart(project: Project) {
        val requestNo = synchronized(this) {
            indicator?.cancel()
            indicator = null
            ++lastRequestNo
        }
        DumbService.getInstance(project).runWhenSmart { runInSmartMode(project, requestNo) }
    }

    private fun runInSmartMode(project: Project, requestNo: Int) {
        val task: KonanTask
        val indicator: BackgroundableProcessIndicator
        synchronized(this) {
            if (requestNo != lastRequestNo) return@runInSmartMode
            task = KonanTask(project)
            indicator = BackgroundableProcessIndicator(task).also { this.indicator = it }
        }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, indicator)
    }

    private class KonanTask(project: Project) : Task.Backgroundable(project, "$indicatorText...", true) {
        private val processed = mutableSetOf<List<OCSymbol>>()
        override fun shouldStartInBackground(): Boolean = true
        override fun onFinished(): Unit = synchronized(this) { indicator = null }

        override fun run(indicator: ProgressIndicator) {
            KtDependencyGraph.getInstance(project).waitForPendingVerifications()
            CidrEventSpan(traceCategory, traceTitle, null).use {
                processTargets(indicator)
            }
        }

        private fun processTargets(indicator: ProgressIndicator): Unit = runNonBlockingReadAction(project, indicator) {
            val bridgeFileManager = KonanBridgeFileManager.getInstance(project)
            val konanTargets = KonanConsumer.getAllReferencedKonanTargets(project)
            indicator.isIndeterminate = false
            indicator.fraction = 0.0

            var i = 0
            val total = (konanTargets.size * KtFileTranslator.PRELOADED_LANGUAGE_KINDS.size).toDouble()
            for (target in konanTargets) {
                indicator.text2 = target.productModuleName
                val bridgeFile = bridgeFileManager.forTarget(target)
                val config = OCInclusionContextUtil.getResolveRootAndActiveConfiguration(bridgeFile, project).configuration ?: continue
                val bridgePsiFile = PsiManager.getInstance(project).findFile(bridgeFile) as? KonanBridgePsiFile ?: continue
                for (kind in KtFileTranslator.PRELOADED_LANGUAGE_KINDS) {
                    indicator.text2 = "${target.productModuleName} (${kind.shortDisplayName})"
                    val context = OCInclusionContext.beforePCHFileContext(config, kind, bridgePsiFile)
                    val bridgingTable = FileSymbolTable.forFile(bridgeFile, context) ?: continue
                    processSymbols(bridgingTable.contents, context)
                    indicator.fraction = ++i / total
                }
            }
        }

        private fun processSymbols(symbols: List<OCSymbol>, context: OCInclusionContext) {
            if (symbols.isEmpty() || !processed.add(symbols)) return
            for (symbol in symbols) when (symbol) {
                is OCModuleImportSymbol -> OCModuleResolver.processImportedHeaders(context, symbol.nameParts) {
                    findTableForInclude(context, it, true)
                    true
                }
                is OCIncludeSymbol -> {
                    symbol.enterNamespace(context)
                    findTableForInclude(context, symbol.targetFile, symbol.isOnce) // one level deep is enough
                    symbol.exitNamespace(context)
                }
                is OCMacroSymbol -> context.define(symbol)
                is OCUndefMacroSymbol -> context.undef(symbol.getName())
            }
        }

        private fun findTableForInclude(context: OCInclusionContext, includeFile: VirtualFile?, once: Boolean): FileSymbolTable? {
            if (includeFile == null || !includeFile.isValid) return null
            if (!SymbolTableProvider.isSourceFile(context.project, includeFile)) return null
            if (!context.reserveInclude(includeFile, once)) return null
            return FileSymbolTablesCache.getInstance(context.project).forFile(includeFile, context)
        }

        private inline fun runNonBlockingReadAction(
            project: Project, indicator: ProgressIndicator, smartModeRequired: Boolean = true, crossinline action: () -> Unit
        ) {
            ReadAction
                .nonBlocking { action() }
                .expireWith(KtDependencyGraph.getInstance(project))
                .wrapProgress(indicator)
                .let { if (smartModeRequired) it.inSmartMode(project) else it }
                .executeSynchronously()
        }
    }
}