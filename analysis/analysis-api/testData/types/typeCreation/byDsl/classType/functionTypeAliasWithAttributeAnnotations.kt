// WITH_FIR_TEST_COMPILER_PLUGIN
annotation class MyAnno

typealias MyAlias = (String) -> Int

fun foo(xx: MyAlias) {
    x<caret_alias>x.toString()
}
