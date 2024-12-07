// ISSUE: KT-68975
// See same test for codegen: compiler/testData/codegen/box/js/inlinedReturnBreakContinue/nonLocalReturn.kt
inline fun foo(makeInt: () -> Int): Int {
    return js("makeInt()")
}

fun box(): String {
    val i: Int = foo {
        return "OK"
    }
    error("unreachable: $i")
}
