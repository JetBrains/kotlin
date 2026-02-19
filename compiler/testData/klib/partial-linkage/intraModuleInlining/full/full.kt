inline fun inlineFunctionFull() = "inlineFunctionFull.v1"

inline fun inlineFunctionWithParamFull(param: String = "defaultFull.v1") = "inlineFunctionWithParamFull.v1: $param"

context(c: String)
inline fun String.inlineExtensionFunctionFull() = "$this.inlineExtensionFunctionFull.v1 with context $c"

inline val inlinePropertyFull: String
    get() = "inlinePropertyFull.v1"

context(c: String)
inline val String.inlineExtensionPropertyFull: String
    get() = "$this.inlineExtensionPropertyFull.v1 with context $c"


class C {
    inline fun inlineClassFunctionFull() = "inlineClassFunctionFull.v1"

    inline fun inlineClassFunctionWithParamFull(param: String = "defaultFull.v1") = "inlineClassFunctionWithParamFull.v1: $param"

    context(c: String)
    inline fun String.inlineClassExtensionFunctionFull() = "$this.inlineClassExtensionFunctionFull.v1 with context $c"

    inline val inlineClassPropertyFull: String
        get() = "inlineClassPropertyFull.v1"

    context(c: String)
    inline val String.inlineClassExtensionPropertyFull: String
        get() = "$this.inlineClassExtensionPropertyFull.v1 with context $c"
}
