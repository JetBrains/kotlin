import kotlin.internal.ExcludedFromFirstStageInlining

@ExcludedFromFirstStageInlining
inline fun inlineFunction() = "inlineFunction.v1"

@ExcludedFromFirstStageInlining
inline fun inlineFunctionWithParam(param: String = "default.v1") = "inlineFunctionWithParam.v1: $param"

@ExcludedFromFirstStageInlining
context(c: String)
inline fun String.inlineExtensionFunction() = "$this.inlineExtensionFunction.v1 with context $c"

class C {
    @ExcludedFromFirstStageInlining
    inline fun inlineClassFunction() = "inlineClassFunction.v1"

    @ExcludedFromFirstStageInlining
    inline fun inlineClassFunctionWithParam(param: String = "default.v1") = "inlineClassFunctionWithParam.v1: $param"

    @ExcludedFromFirstStageInlining
    context(c: String)
    inline fun String.inlineClassExtensionFunction() = "$this.inlineClassExtensionFunction.v1 with context $c"
}
