// !LANGUAGE: +NewInference
// IGNORE_BACKEND: JVM_IR, JS_IR
// WITH_RUNTIME

class Foo<C : Any> {
    fun test(candidates: Collection<C>): List<C> {
        return candidates.sortedBy { 1 }
    }
}

fun box(): String {
    val foo = Foo<String>()

    val list = listOf("OK")
    val sorted = foo.test(list)

    return list.first()
}