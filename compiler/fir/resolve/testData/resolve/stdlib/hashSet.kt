import java.util.HashSet

val a: MutableSet<String>? = HashSet()

var b: MutableSet<String>? = null
    set(_) {
        field = HashSet()
    }

var <T> MutableSet<T>.d: T? get() = null
    set(_) {}

fun <T> produce(): T = TODO()

fun foo() {
    var c: MutableSet<String>? = null
    c = HashSet()

    c!!.d = produce()
}