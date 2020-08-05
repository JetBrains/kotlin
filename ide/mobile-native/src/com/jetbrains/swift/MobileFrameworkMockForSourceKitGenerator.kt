/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.swift

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.util.concurrency.FutureResult
import com.intellij.util.concurrency.QueueProcessor
import com.jetbrains.cidr.apple.gradle.AppleProjectDataService
import com.jetbrains.cidr.apple.gradle.AppleProjectModel
import com.jetbrains.cidr.apple.gradle.AppleTargetModel
import com.jetbrains.cidr.apple.gradle.GradleAppleWorkspace
import com.jetbrains.cidr.lang.OCLog
import com.jetbrains.cidr.lang.preprocessor.OCInclusionContextUtil
import com.jetbrains.mobile.bridging.MobileKonanSwiftModule
import org.jetbrains.konan.resolve.konan.KonanBridgeVirtualFile
import org.jetbrains.konan.resolve.translation.findAllSourceRootsDirectories
import org.jetbrains.konan.resolve.translation.generateObjCHeaderLines
import org.jetbrains.kotlin.analyzer.ModuleDescriptorListener
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.concurrent.TimeUnit

private sealed class Request
private class GenerateRequest(val swiftFile: PsiFile, val future: FutureResult<Boolean>?) : Request() {
    fun cancel() {
        future?.set(false)
    }

    override fun toString(): String =
        "GenerateRequest: file - ${swiftFile.name}, has future - ${future != null}"
}

private class InvalidateRequest() : Request()

interface MobileFrameworkMockForSourceKitGenerator {
    // Comes from any thread
    // Should be fast
    fun invalidate()

    // Comes from any thread
    // Should be async and be able to wait for result
    // Should return immediately if state is valid
    fun generateIfInvalid(swiftFile: PsiFile, future: FutureResult<Boolean>? = null)
}

class MobileFrameworkMockForSourceKitGeneratorImpl(val project: Project) : MobileFrameworkMockForSourceKitGenerator, ModuleDescriptorListener {
    companion object {
        fun getInstance(project: Project): MobileFrameworkMockForSourceKitGeneratorImpl = project.service()
        val LOG = OCLog.LOG
        private const val ROOT_DIRECTORY_NAME = "mock-sourcekit"

        fun baseDir(target: AppleTargetModel): File =
            System.getProperty("xcodeProjBaseDir")?.let { File(it) }
                ?: File(target.editableXcodeProjectDir.parentFile.parentFile, ROOT_DIRECTORY_NAME)

        fun findTarget(project: Project): AppleTargetModel? {
            var projectModel: AppleProjectModel? = null
            AppleProjectDataService.forEachProject(project) { appleProjectModel, _, _ ->
                projectModel = appleProjectModel
            }
            return projectModel?.targets?.values?.firstOrNull()
        }
    }

    private enum class State {
        INVALID,
        VALID
    }

    @Volatile
    private var state = State.INVALID

    private val queue = QueueProcessor<Pair<MobileFrameworkMockForSourceKitGeneratorImpl, Request>> {
        val (generator, request) = it
        generator.doPerformRequest(request)
    }

    init {
        project.messageBus.connect().subscribe(ModuleDescriptorListener.TOPIC, this)
    }

    override fun moduleDescriptorInvalidated(moduleDescriptor: ModuleDescriptor) {
        invalidate()
    }

    override fun invalidate() {
        queue.add(Pair(this, InvalidateRequest()))
    }

    override fun generateIfInvalid(swiftFile: PsiFile, future: FutureResult<Boolean>?) {
        queue.add(Pair(this, GenerateRequest(swiftFile, future)))
    }

    private fun doPerformRequest(request: Request) {
        LOG.debug("Will perform request: $request")

        when (request) {
            is GenerateRequest -> {
                if (queue.hasPendingItemsToProcess()) request.cancel()
                else doGenerateIfInvalid(request.swiftFile, request.future)
            }
            is InvalidateRequest -> doInvalidate()
        }
    }

    private fun doInvalidate() {
        state = State.INVALID
    }

    private fun doGenerateIfInvalid(swiftFile: PsiFile, future: FutureResult<Boolean>?) {
        if (state == State.VALID) {
            LOG.debug("Generating is not needed, state is valid")
            future?.set(true)
            return
        }

        try {
            val sinceTimestamp = System.nanoTime()
            doGenerate(swiftFile)
            LOG.debug("Generated mock framework for SourceKit: ${duration(sinceTimestamp)}")
            state = State.VALID
            future?.set(true)
        }
        catch (e: Throwable) {
            LOG.warn("Failed to generate mock framework for SourceKit: $e")
            state = State.INVALID
            future?.setException(e)
        }
    }

    private fun doGenerate(swiftFile: PsiFile) {
        swiftFile.isValid
        val config = OCInclusionContextUtil.getActiveConfiguration(swiftFile) ?: return
        val project = config.project
        val appleTarget = findTarget(project) ?: return
        val konanSwiftModules =
            GradleAppleWorkspace.getInstance(project).availableKonanFrameworkTargets.values.map { MobileKonanSwiftModule(it, config) }

        konanSwiftModules.forEach {
            doGenerateForModule(appleTarget, project, it)
        }
    }

    private fun doGenerateForModule(target: AppleTargetModel, project: Project, module: MobileKonanSwiftModule) {
        val frameworkName = module.konanBridgeFile().target.productModuleName
        val frameworkDir = File(baseDir(target), "$frameworkName.framework")
        frameworkDir.deleteRecursively()

        val headersDir = File(frameworkDir, "Headers")
        headersDir.mkdirs()
        val headerName = "$frameworkName.h"
        val header = File(headersDir, headerName)
        header.createNewFile()
        val headerLines = generateHeaderLinesForModule(project, module)
        Files.write(header.toPath(), headerLines, StandardOpenOption.APPEND)

        val moduleMapDir = File(frameworkDir, "Modules")
        moduleMapDir.mkdirs()
        File(moduleMapDir, "module.modulemap").writeText(
            """
            |framework module $frameworkName {
            |    umbrella header "$headerName"
            |
            |    export *
            |    module * { export * }
            |}
            """.trimMargin()
        )
    }

    private fun generateHeaderLinesForModule(project: Project, module: MobileKonanSwiftModule): List<String> {
        val bridgeFile = module.konanBridgeFile()
        val frameworkName = bridgeFile.target.productModuleName
        val resolutionFacade = findAnyKtFile(project, bridgeFile)?.getResolutionFacade() ?: return emptyList()

        return generateObjCHeaderLines(frameworkName, resolutionFacade)
    }

    private fun findAnyKtFile(project: Project, konanFile: KonanBridgeVirtualFile): KtFile? {
        val virtualFile = findAllSourceRootsDirectories(project, konanFile).mapNotNull {
            findAnyKotlinVirtualFile(it)
        }.firstOrNull() ?: return null

        return ApplicationManager.getApplication().runReadAction<KtFile?> {
            (PsiManager.getInstance(project).findFile(virtualFile) as? KtFile)
        }
    }

    private fun findAnyKotlinVirtualFile(sourceRoot: VirtualFile): VirtualFile? {
        var result: VirtualFile? = null

        VfsUtilCore.visitChildrenRecursively(sourceRoot, object : VirtualFileVisitor<Nothing>() {
            override fun visitFileEx(file: VirtualFile): Result {
                if (file.fileType == KotlinFileType.INSTANCE) {
                    result = file
                    return skipTo(sourceRoot)
                }
                return CONTINUE
            }
        })

        return result
    }

    private fun duration(sinceNano: Long): String =
        "${TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - sinceNano)} ms"
}