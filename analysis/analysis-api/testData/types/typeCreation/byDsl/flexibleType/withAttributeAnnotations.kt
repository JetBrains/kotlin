// WITH_FIR_TEST_COMPILER_PLUGIN
annotation class MyAnno

fun foo(xx: Any, yy: Any?) {
    x<caret_lower>x.toString()
    y<caret_upper>y.toString()
}
