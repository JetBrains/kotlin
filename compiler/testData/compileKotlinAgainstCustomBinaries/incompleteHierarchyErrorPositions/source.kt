import test.Sub;

class SubSub : Sub()
class Client<T : Sub>(val prop: T)
fun <T : Sub> withTypeParam() {}

fun withCallRefArg(arg: Sub.() -> String) {}

fun Sub.extension() {}

fun test() {
    Sub().unresolved()
    SubSub().unresolved()
    val obj = object : Sub() {}
    withCallRefArg(Sub::resolved)
    Sub().resolved()
    Sub().extension()
}
