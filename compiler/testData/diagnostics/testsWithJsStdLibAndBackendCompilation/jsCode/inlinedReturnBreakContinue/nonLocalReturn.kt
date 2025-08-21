// ISSUE: KT-68975
// LANGUAGE: +IrInlinerBeforeKlibSerialization
inline fun foo(<!UNUSED_PARAMETER!>makeInt<!>: () -> Int): Int {
    return js("makeInt()")
}

fun box(): String {
    val i: Int = foo {
        return "OK"
    }
    error("unreachable: $i")
}
