// RUN_PIPELINE_TILL: FRONTEND
// MODULE: disabledJvmDefaults
// KOTLINC_ARGS: -jvm-default=disable
// JVM_DEFAULT_MODE: disable
// FILE: source.kt
interface IrGeneratorContext {
    val irFactory: Any get() = Any()
}

// MODULE: enabledJvmDefaults(disabledJvmDefaults)
// KOTLINC_ARGS: -jvm-default=no-compatibility
// JVM_DEFAULT_MODE: no-compatibility
// FILE: source.kt
@RequiresOptIn
annotation class ObsoleteDescriptorBasedAPI

interface IrPluginContext: IrGeneratorContext {
    @ObsoleteDescriptorBasedAPI
    val optInModuleDescriptor: Any
    @Deprecated("", level = DeprecationLevel.ERROR)
    val deprecatedModuleDescriptor: Any
}

// MODULE: main(disabledJvmDefaults, enabledJvmDefaults)
// FILE: source.kt
fun test(pluginContext: IrPluginContext) {
    pluginContext.optInModuleDescriptor
    pluginContext.deprecatedModuleDescriptor
}
