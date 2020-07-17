class Inv<E>

typealias MyAlias = Inv<out CharSequence>

fun foo(p: MyAlias) {
    bar(p).length
}

fun <T : Any> bar(x: Inv<T>): T = TODO()
