// "Create function 'compareTo' from usage" "true"

class A<T>(val n: T) {
    fun compareTo(arg: T): Int {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

fun test() {
    A(1) < 2
}