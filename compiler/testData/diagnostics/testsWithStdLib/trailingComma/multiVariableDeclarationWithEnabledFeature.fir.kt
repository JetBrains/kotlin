// !DIAGNOSTICS: -UNUSED_VARIABLE, -UNUSED_DESTRUCTURED_PARAMETER_ENTRY, -UNUSED_ANONYMOUS_PARAMETER
// !LANGUAGE: +TrailingCommas

data class Foo1(val x: String, val y: String, val z: String = "")

fun main() {
    val (x1,y1,) = Pair(1, 2)
    val (x2, y2: Number,) = Pair(1,2)
    val (x3,y3,z3,) = Foo1("", "",)
    val (x4,y4: CharSequence,) = Foo1("", "", "",)
    val (x41,y41: CharSequence,/**/) = Foo1("", "", "",)

    val x5: (Pair<Int, Int>, Int) -> Unit = { (x,y,),z, -> }
    val x6: (Foo1, Int) -> Any = { (x,y,z: CharSequence,), z2: Number, -> 1 }
    val x61: (Foo1, Int) -> Any = { (x,y,z: CharSequence,/**/), z2: Number,/**/ -> 1 }

    for ((i, j) in listOf(Pair(1,2))) {}
    for ((i: Any,) in listOf(Pair(1,2))) {}
    for ((i: Any,/**/) in listOf(Pair(1,2))) {}
}
