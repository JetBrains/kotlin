// FIR_IDENTICAL

// MUTE_SIGNATURE_COMPARISON_K2: JS_IR
// ^ KT-57818

fun <T> castFun(x: Any) = x as T

fun <T> Any.castExtFun() = this as T

val <T> T.castExtVal
    get() = this as T


class Host<T> {
    fun castMemberFun(x: Any) = x as T

    fun <TF> castGenericMemberFun(x: Any) = x as TF

    fun Any.castMemberExtFun() = this as T

    fun <TF> Any.castGenericMemberExtFun() = this as TF

    val Any.castMemberExtVal
        get() = this as T

    val <TV> TV.castGenericMemberExtVal
        get() = this as TV
}

