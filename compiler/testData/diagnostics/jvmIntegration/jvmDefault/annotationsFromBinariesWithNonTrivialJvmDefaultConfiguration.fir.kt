// MODULE: disabledJvmDefaults
// KOTLINC_ARGS: -Xjvm-default=disable
// FILE: source.kt
interface IrGeneratorContext {
    val irFactory: Any get() = Any()
}

// MODULE: enabledJvmDefaults(disabledJvmDefaults)
// KOTLINC_ARGS: -Xjvm-default=all
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
    pluginContext.<!OPT_IN_USAGE_ERROR!>optInModuleDescriptor<!>
    pluginContext.<!DEPRECATION_ERROR!>deprecatedModuleDescriptor<!>
}
