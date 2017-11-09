data class SomeName(val a: Int, val b: Int, val c: String)

fun SomeName.func() = <caret>copy(1, 0)