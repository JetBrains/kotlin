// "Create function 'plusAssign' from usage" "true"

class A<T>(val n: T) {
    fun plusAssign(arg: T) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

fun test() {
    A(1) += 2
}