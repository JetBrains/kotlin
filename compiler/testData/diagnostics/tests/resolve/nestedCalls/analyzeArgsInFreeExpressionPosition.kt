class A {
    default object {

    }
}

fun use(vararg a: Any?) = a

fun test() {
    use(use(A, null).toString())
}

