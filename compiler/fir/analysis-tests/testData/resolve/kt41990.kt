// ISSUE: KT-41990

fun getLambda(): String.() -> Unit = null!!

fun String.test_1(s: String) {
    getLambda()()
    getLambda()(s)
}
