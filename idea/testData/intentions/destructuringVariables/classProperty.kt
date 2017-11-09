// IS_APPLICABLE: false

data class XY(val x: Int, val y: Int)

fun create() = XY(1, 2)

class Foo {
    val xy = <caret>create()
}