@RequiresOptIn
annotation class ObsoleteDescriptorBasedAPI

interface IrPluginContext: IrGeneratorContext {
    @ObsoleteDescriptorBasedAPI
    val optInModuleDescriptor: Any
    @Deprecated("", level = DeprecationLevel.ERROR)
    val deprecatedModuleDescriptor: Any
}
