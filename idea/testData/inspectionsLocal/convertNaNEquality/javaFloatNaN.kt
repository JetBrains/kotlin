// RUNTIME_WITH_FULL_JDK

fun test() {
    val a = 0.5f
    val x = a ==<caret> java.lang.Float.NaN
}