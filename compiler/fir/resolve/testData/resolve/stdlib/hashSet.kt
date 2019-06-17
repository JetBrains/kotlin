import java.util.HashSet

val a: MutableSet<String>? = HashSet()

var b: MutableSet<String>? = null
    set(_) {
        field = HashSet()
    }

fun foo() {
    var c: MutableSet<String>? = null
    c = HashSet()
}