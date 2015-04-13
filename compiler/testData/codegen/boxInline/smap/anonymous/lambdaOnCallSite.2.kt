package builders

inline fun call(inlineOptions(InlineOption.ONLY_LOCAL_RETURN) init: () -> Unit) {
    return init()
}
//NO_CHECK_LAMBDA_INLINING
//SMAP ABSENT
