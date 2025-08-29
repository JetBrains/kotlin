inline fun inlineFunction() = "inlineFunction.v1"

inline fun inlineFunctionWithParam(param: String = "default.v1") = "inlineFunctionWithParam.v1: $param"

context(c: String)
inline fun String.inlineExtensionFunction() = "$this.inlineExtensionFunction.v1 with context $c"

inline val inlineProperty: String
    get() = "inlineProperty.v1"

context(c: String)
inline val String.inlineExtensionProperty: String
    get() = "$this.inlineExtensionProperty.v1 with context $c"


class C {
    inline fun inlineClassFunction() = "inlineClassFunction.v1"

    inline fun inlineClassFunctionWithParam(param: String = "default.v1") = "inlineClassFunctionWithParam.v1: $param"

    context(c: String)
    inline fun String.inlineClassExtensionFunction() = "$this.inlineClassExtensionFunction.v1 with context $c"

    inline val inlineClassProperty: String
        get() = "inlineClassProperty.v1"

    context(c: String)
    inline val String.inlineClassExtensionProperty: String
        get() = "$this.inlineClassExtensionProperty.v1 with context $c"
}
