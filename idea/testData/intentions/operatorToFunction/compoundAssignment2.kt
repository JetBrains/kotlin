// WITH_RUNTIME
interface C {
    operator fun get(p: String): MutableList<Int>
}

fun foo(c: C) {
    c<caret>[""] += 10
}