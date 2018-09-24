package dependency

typealias A = () -> Unit

fun foo(a: A) {
    a.invoke()
}

class SomeClass