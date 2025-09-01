inline fun inlineFunctionIntraModule() = "inlineFunctionIntraModule.v1"

inline fun inlineFunctionWithParamIntraModule(param: String = "defaultIntraModule.v1") = "inlineFunctionWithParamIntraModule.v1: $param"

context(c: String)
inline fun String.inlineExtensionFunctionIntraModule() = "$this.inlineExtensionFunctionIntraModule.v1 with context $c"

inline val inlinePropertyIntraModule: String
    get() = "inlinePropertyIntraModule.v1"

context(c: String)
inline val String.inlineExtensionPropertyIntraModule: String
    get() = "$this.inlineExtensionPropertyIntraModule.v1 with context $c"


class E {
    inline fun inlineClassFunctionIntraModule() = "inlineClassFunctionIntraModule.v1"

    inline fun inlineClassFunctionWithParamIntraModule(param: String = "defaultIntraModule.v1") = "inlineClassFunctionWithParamIntraModule.v1: $param"

    context(c: String)
    inline fun String.inlineClassExtensionFunctionIntraModule() = "$this.inlineClassExtensionFunctionIntraModule.v1 with context $c"

    inline val inlineClassPropertyIntraModule: String
        get() = "inlineClassPropertyIntraModule.v1"

    context(c: String)
    inline val String.inlineClassExtensionPropertyIntraModule: String
        get() = "$this.inlineClassExtensionPropertyIntraModule.v1 with context $c"
}
