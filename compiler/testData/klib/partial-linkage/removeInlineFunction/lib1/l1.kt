inline fun topLevelInlineFunction() = "topLevelInlineFunction"

inline fun topLevelInlineFunctionWithParam(p: String) = p

context(c: String)
inline fun String.topLevelInlineFunctionWithReceiver() = this + c

class C {
    inline fun classlInlineFunction() = "classlInlineFunction"

    inline fun classlInlineFunctionWithParam(p: String) = p

    context(c: String)
    inline fun String.classlInlineFunctionWithReceiver() = this + c
}