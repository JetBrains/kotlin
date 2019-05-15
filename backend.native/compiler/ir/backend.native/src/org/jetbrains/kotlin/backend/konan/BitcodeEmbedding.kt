package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.llvm.Int8
import org.jetbrains.kotlin.backend.konan.llvm.Llvm
import org.jetbrains.kotlin.konan.target.CompilerOutputKind

object BitcodeEmbedding {

    enum class Mode {
        NONE, MARKER, FULL
    }

    internal fun getLinkerOptions(config: KonanConfig): List<String> = when (config.bitcodeEmbeddingMode) {
        Mode.NONE -> emptyList()
        Mode.MARKER -> listOf("-bitcode_bundle", "-bitcode_process_mode", "marker")
        Mode.FULL -> listOf("-bitcode_bundle")
    }

    internal fun getClangOptions(config: KonanConfig): List<String> = when (config.bitcodeEmbeddingMode) {
        BitcodeEmbedding.Mode.NONE -> listOf("-fembed-bitcode=off")
        BitcodeEmbedding.Mode.MARKER -> listOf("-fembed-bitcode=marker")
        BitcodeEmbedding.Mode.FULL -> listOf("-fembed-bitcode=all")
    }

    private val KonanConfig.bitcodeEmbeddingMode get() = configuration.get(KonanConfigKeys.BITCODE_EMBEDDING_MODE)!!.also {
        require(it == Mode.NONE || this.produce == CompilerOutputKind.FRAMEWORK) {
            "${it.name.toLowerCase()} bitcode embedding mode is not supported when producing ${this.produce.name.toLowerCase()}"
        }
    }

    internal fun processModule(llvm: Llvm) = when (llvm.context.config.bitcodeEmbeddingMode) {
        Mode.NONE -> {}
        Mode.MARKER -> {
            addEmptyMarker(llvm, "konan_llvm_bitcode", "__LLVM,__bitcode")
            addEmptyMarker(llvm, "konan_llvm_cmdline", "__LLVM,__cmdline")
        }
        Mode.FULL -> {
            addEmptyMarker(llvm, "konan_llvm_asm", "__LLVM,__asm")
        }
    }

    private fun addEmptyMarker(llvm: Llvm, name: String, section: String) {
        val global = llvm.staticData.placeGlobal(name, Int8(0), isExported = false)
        global.setSection(section)
        llvm.usedGlobals += global.llvmGlobal
    }
}