// "Create function 'invoke' from usage" "true"

class A<T>(val n: T)

fun test(): A<String> {
    return 1(2, "2")
}

fun Int.invoke(i: Int, s: String): A<String> {
    throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
}