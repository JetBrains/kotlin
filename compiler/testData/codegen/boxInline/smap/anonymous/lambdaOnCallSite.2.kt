package builders

inline fun call(crossinline init: () -> Unit) {
    return init()
}
//NO_CHECK_LAMBDA_INLINING
//SMAP ABSENT
