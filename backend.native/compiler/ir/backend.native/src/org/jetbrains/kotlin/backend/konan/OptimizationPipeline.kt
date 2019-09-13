package org.jetbrains.kotlin.backend.konan

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import llvm.*
import org.jetbrains.kotlin.konan.target.KonanTarget

private fun initializeLlvmGlobalPassRegistry() {
    val passRegistry = LLVMGetGlobalPassRegistry()

    LLVMInitializeCore(passRegistry)
    LLVMInitializeTransformUtils(passRegistry)
    LLVMInitializeScalarOpts(passRegistry)
    LLVMInitializeVectorization(passRegistry)
    LLVMInitializeInstCombine(passRegistry)
    LLVMInitializeIPO(passRegistry)
    LLVMInitializeInstrumentation(passRegistry)
    LLVMInitializeAnalysis(passRegistry)
    LLVMInitializeIPA(passRegistry)
    LLVMInitializeCodeGen(passRegistry)
    LLVMInitializeTarget(passRegistry)
}

internal fun shouldRunLateBitcodePasses(context: Context): Boolean {
    return context.coverage.enabled
}

internal fun runLateBitcodePasses(context: Context, llvmModule: LLVMModuleRef) {
    val passManager = LLVMCreatePassManager()!!
    LLVMKotlinAddTargetLibraryInfoWrapperPass(passManager, context.llvm.targetTriple)
    context.coverage.addLateLlvmPasses(passManager)
    LLVMRunPassManager(passManager, llvmModule)
    LLVMDisposePassManager(passManager)
}

private class LlvmPipelineConfiguration(context: Context) {

    private val target = context.config.target

    val targetTriple: String = context.llvm.targetTriple

    // Some of these values are copied from corresponding runtime.bc
    // which is using "generic" target CPU for many case.
    // This approach is suboptimal because target-cpu="generic" limits
    // the set of used cpu features.
    // TODO: refactor KonanTarget so that we can explicitly specify
    //  target cpu or arch+features combination in a single place.
    val cpuModel: String = when (target) {
        KonanTarget.IOS_ARM32 -> "generic"
        KonanTarget.IOS_ARM64 -> "cyclone"
        KonanTarget.IOS_X64 -> "core2"
        KonanTarget.TVOS_ARM64 -> "cyclone"
        KonanTarget.TVOS_X64 -> "core2"
        KonanTarget.WATCHOS_X86 -> "i386"
        KonanTarget.WATCHOS_X64 -> "core2"
        KonanTarget.WATCHOS_ARM64,
        KonanTarget.WATCHOS_ARM32 -> "cortex-a7"
        KonanTarget.LINUX_X64 -> "x86-64"
        KonanTarget.MINGW_X86 -> "pentium4"
        KonanTarget.MINGW_X64 -> "x86-64"
        KonanTarget.MACOS_X64 -> "core2"
        KonanTarget.LINUX_ARM32_HFP -> "arm1136jf-s"
        KonanTarget.LINUX_ARM64 -> "generic"
        KonanTarget.ANDROID_ARM32 -> "arm7tdmi"
        KonanTarget.ANDROID_ARM64 -> "cortex-a57"
        KonanTarget.ANDROID_X64 -> "x86-64"
        KonanTarget.ANDROID_X86 -> "i686"
        KonanTarget.LINUX_MIPS32 -> "mips32r2"
        KonanTarget.LINUX_MIPSEL32 -> "mips32r2"
        KonanTarget.WASM32,
        is KonanTarget.ZEPHYR -> error("There is no support for ${target.name} target yet.")
    }

    val cpuFeatures: String = when (target) {
        KonanTarget.LINUX_ARM32_HFP -> "+dsp,+strict-align,+vfp2,-crypto,-d16,-fp-armv8,-fp-only-sp,-fp16,-neon,-thumb-mode,-vfp3,-vfp4"
        KonanTarget.ANDROID_ARM32 -> "+soft-float,+strict-align,-crypto,-neon,-thumb-mode"
        else -> ""
    }

    val customInlineThreshold: Int? = when {
        context.shouldOptimize() -> 100
        context.shouldContainDebugInfo() -> null
        else -> null
    }

    val optimizationLevel: Int = when {
        context.shouldOptimize() -> 3
        context.shouldContainDebugInfo() -> 0
        else -> 1
    }

    val sizeLevel: Int = when {
        context.shouldOptimize() -> 0
        context.shouldContainDebugInfo() -> 0
        else -> 0
    }

    val codegenOptimizationLevel: LLVMCodeGenOptLevel = when {
        context.shouldOptimize() -> LLVMCodeGenOptLevel.LLVMCodeGenLevelAggressive
        context.shouldContainDebugInfo() -> LLVMCodeGenOptLevel.LLVMCodeGenLevelNone
        else -> LLVMCodeGenOptLevel.LLVMCodeGenLevelDefault
    }

    val relocMode: LLVMRelocMode = LLVMRelocMode.LLVMRelocDefault

    val codeModel: LLVMCodeModel = LLVMCodeModel.LLVMCodeModelDefault
}

// Since we are in a "closed world" internalization and global dce
// can be safely used to reduce size of a bitcode.
internal fun runClosedWorldCleanup(context: Context) {
    initializeLlvmGlobalPassRegistry()
    val llvmModule = context.llvmModule!!
    val modulePasses = LLVMCreatePassManager()
    if (context.llvmModuleSpecification.isFinal) {
        LLVMAddInternalizePass(modulePasses, 0)
    }
    LLVMAddGlobalDCEPass(modulePasses)
    LLVMRunPassManager(modulePasses, llvmModule)
    LLVMDisposePassManager(modulePasses)
}

internal fun runLlvmOptimizationPipeline(context: Context) {
    val llvmModule = context.llvmModule!!
    val config = LlvmPipelineConfiguration(context)

    memScoped {
        LLVMKotlinInitializeTargets()

        initializeLlvmGlobalPassRegistry()
        val passBuilder = LLVMPassManagerBuilderCreate()
        val modulePasses = LLVMCreatePassManager()
        LLVMPassManagerBuilderSetOptLevel(passBuilder, config.optimizationLevel)
        LLVMPassManagerBuilderSetSizeLevel(passBuilder, config.sizeLevel)
        // TODO: use LLVMGetTargetFromName instead.
        val target = alloc<LLVMTargetRefVar>()
        val foundLlvmTarget = LLVMGetTargetFromTriple(config.targetTriple, target.ptr, null) == 0
        check(foundLlvmTarget) { "Cannot get target from triple ${config.targetTriple}." }

        val targetMachine = LLVMCreateTargetMachine(
                target.value,
                config.targetTriple,
                config.cpuModel,
                config.cpuFeatures,
                config.codegenOptimizationLevel,
                config.relocMode,
                config.codeModel)

        LLVMKotlinAddTargetLibraryInfoWrapperPass(modulePasses, config.targetTriple)
        // TargetTransformInfo pass.
        LLVMAddAnalysisPasses(targetMachine, modulePasses)
        if (context.llvmModuleSpecification.isFinal) {
            // Since we are in a "closed world" internalization can be safely used
            // to reduce size of a bitcode with global dce.
            LLVMAddInternalizePass(modulePasses, 0)
        }
        LLVMAddGlobalDCEPass(modulePasses)

        config.customInlineThreshold?.let { threshold ->
            LLVMPassManagerBuilderUseInlinerWithThreshold(passBuilder, threshold)
        }
        // Pipeline that is similar to `llvm-lto`.
        // TODO: Add ObjC optimization passes.
        LLVMPassManagerBuilderPopulateLTOPassManager(passBuilder, modulePasses, Internalize = 0, RunInliner = 1)

        LLVMRunPassManager(modulePasses, llvmModule)

        LLVMPassManagerBuilderDispose(passBuilder)
        LLVMDisposeTargetMachine(targetMachine)
        LLVMDisposePassManager(modulePasses)
    }
    if (shouldRunLateBitcodePasses(context)) {
        runLateBitcodePasses(context, llvmModule)
    }
}