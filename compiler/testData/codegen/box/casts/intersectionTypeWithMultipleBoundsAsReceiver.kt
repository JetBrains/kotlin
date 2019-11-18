// IGNORE_BACKEND_FIR: JVM_IR
interface Foo<T>
interface Bar<T>

class Baz<T> : Foo<T>, Bar<T>

fun <T, S> S.bip(): String where S : Foo<T>, S: Bar<T> {
    return "OK"
}

fun box(): String {
    val baz = Baz<String>()
    return baz.bip()
}