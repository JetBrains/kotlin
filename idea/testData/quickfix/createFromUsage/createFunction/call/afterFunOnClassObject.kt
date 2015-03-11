// "Create function 'foo'" "true"

class A<T>(val n: T) {
    default object {
        fun foo(i: Int): Int {
            throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }
}

fun test() {
    val a: Int = A.foo(2)
}