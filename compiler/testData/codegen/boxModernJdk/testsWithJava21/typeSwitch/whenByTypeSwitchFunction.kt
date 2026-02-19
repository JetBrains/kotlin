// TARGET_BACKEND: JVM
// WHEN_EXPRESSIONS: INDY

// CHECK_BYTECODE_TEXT
// 0 INSTANCEOF
// 1 INVOKEDYNAMIC typeSwitch
// 1 kotlin.Function.class
// 2 KRunnableSam.class

fun interface KRunnableSam {
    fun invoke()
}

fun foo(x: Any): Int {
    return when (x) {
        is Function<*> -> 1
        is KRunnableSam -> 2
        else -> 100
    }
}


fun box(): String {
    if (foo({ 5.toString() == "5" }) != 1) return "1"
    if (foo(KRunnableSam { 6.toDouble() }) != 2) return "2"

    return "OK"
}