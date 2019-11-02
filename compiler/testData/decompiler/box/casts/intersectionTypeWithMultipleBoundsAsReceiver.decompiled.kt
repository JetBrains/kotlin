interface Foo<T> {
}
interface Bar<T> {
}
class Baz<T>: Foo<T>, Bar<T> {
}
fun <T, S  : Foo<T>, Bar<T>> S.bip() : String  {
    return "OK"
}

fun box() : String  {
    val baz : Baz<String> = Baz<String>< String >()
    return baz.bip<String, Baz<String>>()
}
