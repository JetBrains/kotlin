inline fun inlineFunctionDisabled() = "inlineFunctionDisabled.v1"

inline fun inlineFunctionWithParamDisabled(param: String = "defaultDisabled.v1") = "inlineFunctionWithParamDisabled.v1: $param"

context(c: String)
inline fun String.inlineExtensionFunctionDisabled() = "$this.inlineExtensionFunctionDisabled.v1 with context $c"

inline val inlinePropertyDisabled: String
    get() = "inlinePropertyDisabled.v1"

context(c: String)
inline val String.inlineExtensionPropertyDisabled: String
    get() = "$this.inlineExtensionPropertyDisabled.v1 with context $c"


class D {
    inline fun inlineClassFunctionDisabled() = "inlineClassFunctionDisabled.v1"

    inline fun inlineClassFunctionWithParamDisabled(param: String = "defaultDisabled.v1") = "inlineClassFunctionWithParamDisabled.v1: $param"

    context(c: String)
    inline fun String.inlineClassExtensionFunctionDisabled() = "$this.inlineClassExtensionFunctionDisabled.v1 with context $c"

    inline val inlineClassPropertyDisabled: String
        get() = "inlineClassPropertyDisabled.v1"

    context(c: String)
    inline val String.inlineClassExtensionPropertyDisabled: String
        get() = "$this.inlineClassExtensionPropertyDisabled.v1 with context $c"
}
