// "Create function 'contains' from usage" "true"

class A<T>(val n: T) {
    fun contains(arg: T): Boolean {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

fun test() {
    when {
        2 in A(1) -> {}
    }
}