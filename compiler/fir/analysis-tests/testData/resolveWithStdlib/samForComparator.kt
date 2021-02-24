// FULL_JDK

fun foo(x: Comparator<in CharSequence>) {}


fun main() {
    foo { x, y ->
        x.length - y.length
    }
}
