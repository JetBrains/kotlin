val foo: () -> Unit = {}

fun box(): String {
    foo() as Unit
    return "OK"
}