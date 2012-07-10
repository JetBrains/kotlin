//KT-1185 Support full enumeration check for 'when'

package kt1185

enum class Direction {
    NORTH
    SOUTH
    WEST
    EAST
}

enum class Color(val rgb : Int) {
    RED : Color(0xFF0000)
    GREEN : Color(0x00FF00)
    BLUE : Color(0x0000FF)
}

fun foo(d: Direction) = when(d) { //no 'else' should be requested
    Direction.NORTH -> 1
    Direction.SOUTH -> 2
    Direction.WEST -> 3
    Direction.EAST -> 4
}

fun foo1(d: Direction) = <!NO_ELSE_IN_WHEN!>when<!>(d) {
    Direction.NORTH -> 1
    Direction.SOUTH -> 2
    Direction.WEST -> 3
}

fun bar(c: Color) = when (c) {
    Color.RED -> 1
    Color.GREEN -> 2
    Color.BLUE -> 3
}

fun bar1(c: Color) = <!NO_ELSE_IN_WHEN!>when<!> (c) {
    Color.RED -> 1
    Color.GREEN -> 2
}

open enum class SomeEnum() {}

enum class MyEnum : SomeEnum() {
    A
    B
}

fun g(me: SomeEnum) = <!NO_ELSE_IN_WHEN!>when<!> (me) {
    MyEnum.A -> 1
    MyEnum.B -> 2
}

fun t2(me: MyEnum) = when (me) {
    MyEnum.A -> 1
    MyEnum.B -> 2
}
