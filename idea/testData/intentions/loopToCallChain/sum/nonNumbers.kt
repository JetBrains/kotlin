// WITH_RUNTIME
// IS_APPLICABLE: false
// IS_APPLICABLE_2: false
class C

operator fun C.plus(other: C): C = TODO()

fun foo(list: List<C>): C {
    var s = C()
    <caret>for (item in list) {
        s += item
    }
    return s
}
