// DIAGNOSTICS: -UNUSED_ANONYMOUS_PARAMETER -UNUSED_PARAMETER

fun foo(total: Int, next: Int) = 10
fun foo(total: Int, next: Float) = 10
fun foo(total: Float, next: Int) = 10

fun call(x: String) {}

fun foo(x: Float, y: Float) = {
    var newValue = 1
    newValue += listOf<Int>().asSequence().fold(0) { total, next ->
        call(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>11<!>)
        total + next
    }
    newValue += listOf<Int>().asSequence().fold(0, fun(total, next): Int { return total + next })
    newValue += listOf<Int>().asSequence().fold(0, fun(total, next) = total + next)
    newValue += listOf<Int>().asSequence().fold(0, ::foo)
}

class A {
    operator fun plus(x: Int) = A()
    operator fun plusAssign(x: Float) {}
}

fun <T> id(x: T) = x

fun foo2() = {
    var x = A()
    x += <!TYPE_MISMATCH!>{ "" }<!>
    var y = A()
    y += 1
}

class B {
    operator fun plus(x: () -> String) = A()
    operator fun plusAssign(x: () -> Int) {}
}

fun foo3(x: B) = {
    x += { <!TYPE_MISMATCH, TYPE_MISMATCH!>""<!> }
    x += id { <!TYPE_MISMATCH, TYPE_MISMATCH!>""<!> }
}
