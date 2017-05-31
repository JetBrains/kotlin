interface I<T>

fun <T> Int.foo(p: I<T>){}

fun bar(p: I<String>) {
    1.foo<caret><String>(p)
}