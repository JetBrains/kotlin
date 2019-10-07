// !DIAGNOSTICS: -UNUSED_VARIABLE, -UNUSED_DESTRUCTURED_PARAMETER_ENTRY, -UNUSED_ANONYMOUS_PARAMETER

data class Foo1(val x: String, val y: String, val z: String = "")

fun main() {
    val (x1,y1<!UNSUPPORTED_FEATURE!>,<!>) = Pair(1,2)
    val (x2, y2: Number<!UNSUPPORTED_FEATURE!>,<!>) = Pair(1,2)
    val (x3,y3,z3<!UNSUPPORTED_FEATURE!>,<!>) = Foo1("", ""<!UNSUPPORTED_FEATURE!>,<!> )
    val (x4,y4: CharSequence<!UNSUPPORTED_FEATURE!>,<!>) = Foo1("", "", ""<!UNSUPPORTED_FEATURE!>,<!>)
    val (x41,y41: CharSequence<!UNSUPPORTED_FEATURE!>,<!>/**/) = Foo1("", "", ""<!UNSUPPORTED_FEATURE!>,<!>)

    val x5: (Pair<Int, Int>, Int) -> Unit = { (x,y<!UNSUPPORTED_FEATURE!>,<!>),z<!UNSUPPORTED_FEATURE!>,<!> -> }
    val x6: (Foo1, Int) -> Any = { (x,y,z: CharSequence<!UNSUPPORTED_FEATURE!>,<!>), z1: Number<!UNSUPPORTED_FEATURE!>,<!> -> 1 }
    val x61: (Foo1, Int) -> Any = { (x,y,z: CharSequence<!UNSUPPORTED_FEATURE!>,<!>/**/), z1: Number<!UNSUPPORTED_FEATURE!>,<!>/**/ -> 1 }

    for ((i, j<!UNSUPPORTED_FEATURE!>,<!>) in listOf(Pair(1,2))) {}
    for ((i: Any<!UNSUPPORTED_FEATURE!>,<!>) in listOf(Pair(1,2))) {}
    for ((i: Any<!UNSUPPORTED_FEATURE!>,<!>/**/) in listOf(Pair(1,2))) {}
}
