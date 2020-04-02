interface Base
class Inv<K> : Base

fun foo(x: Int): Inv<Int> = TODO()
fun foo(y: String): Inv<String> = TODO()

fun <T, R : Number> bar(f: (T) -> Inv<R>, p: String = "") {}

fun <T, R : Base> bar(f: (T) -> Inv<R>, p: Int = 4) {}

fun test() {
    bar(::foo)
}
