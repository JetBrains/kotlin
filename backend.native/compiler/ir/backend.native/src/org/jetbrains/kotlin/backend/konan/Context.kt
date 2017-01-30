package org.jetbrains.kotlin.backend.konan

import llvm.*
import org.jetbrains.kotlin.backend.konan.ir.Ir
import org.jetbrains.kotlin.backend.konan.ir.ModuleIndex
import org.jetbrains.kotlin.backend.konan.KonanBackendContext
import org.jetbrains.kotlin.backend.konan.KonanPhase
import org.jetbrains.kotlin.backend.konan.KonanConfig
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.backend.konan.descriptors.deepPrint
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.ir.util.DumpIrTreeVisitor
import java.lang.System.out

internal final class Context(val config: KonanConfig) : KonanBackendContext() {

    val debug = true

    var moduleDescriptor: ModuleDescriptor? = null

    // TODO: make lateinit?
    var irModule: IrModuleFragment? = null
        set(module: IrModuleFragment?) {
            if (field != null) {
                throw Error("Another IrModule in the context.")
            }
            field = module!!

            ir = Ir(this, module)
        }

    lateinit var ir: Ir

    override val irBuiltIns
        get() = ir.irModule.irBuiltins

    var llvmModule: LLVMModuleRef? = null
        set(module: LLVMModuleRef?) {
            if (field != null) {
                throw Error("Another LLVMModule in the context.")
            }
            field = module!!

            llvm = Llvm(this, module)
        }

    lateinit var llvm: Llvm
    lateinit var llvmDeclarations: LlvmDeclarations

    var phase: KonanPhase? = null
    var depth: Int = 0

    protected fun separator(title: String) {
        println("\n\n--- ${title} ----------------------\n")
    }

    fun verifyDescriptors() {
        // TODO: Nothing here for now.
    }

    fun printDescriptors() {
        if (moduleDescriptor == null) return
        separator("Descriptors after: ${phase?.description}")
        moduleDescriptor!!.deepPrint()
    }

    fun verifyIr() {
        if (irModule == null) return
        // TODO: We don't have it yet.
    }

    fun printIr() {
        if (irModule == null) return
        separator("IR after: ${phase?.description}")
        irModule!!.accept(DumpIrTreeVisitor(out), "")
    }

    fun verifyBitCode() {
        if (llvmModule == null) return
        verifyModule(llvmModule!!)
    }

    fun printBitCode() {
        if (llvmModule == null) return
        separator("BitCode after: ${phase?.description}")
        LLVMDumpModule(llvmModule!!)
    }

    fun verify() {
        verifyDescriptors()
        verifyIr()
        verifyBitCode()
    }

    fun print() {
        printDescriptors()
        printIr()
        printBitCode()
    }

    fun shouldVerifyDescriptors(): Boolean {
        return config.configuration.getBoolean(KonanConfigKeys.VERIFY_DESCRIPTORS) 
    }

    fun shouldVerifyIr(): Boolean {
        return config.configuration.getBoolean(KonanConfigKeys.VERIFY_IR) 
    }

    fun shouldVerifyBitCode(): Boolean {
        return config.configuration.getBoolean(KonanConfigKeys.VERIFY_BITCODE) 
    }

    fun shouldPrintDescriptors(): Boolean {
        return config.configuration.getBoolean(KonanConfigKeys.PRINT_DESCRIPTORS) 
    }

    fun shouldPrintIr(): Boolean {
        return config.configuration.getBoolean(KonanConfigKeys.PRINT_IR) 
    }

    fun shouldPrintBitCode(): Boolean {
        return config.configuration.getBoolean(KonanConfigKeys.PRINT_BITCODE) 
    }

    fun shouldProfilePhases(): Boolean {
        return config.configuration.getBoolean(KonanConfigKeys.TIME_PHASES) 
    }

    fun log(message: String) {
        if (phase?.verbose ?: false) {
            println(message)
        }
    }
}

