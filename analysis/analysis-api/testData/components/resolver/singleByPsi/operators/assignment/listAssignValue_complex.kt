// WITH_STDLIB
class Foo

fun check(l: MutableList<Foo>) {
    l[0] +<caret>= 1
}

operator fun <R, P> R.plus(other: P): R = null!!
