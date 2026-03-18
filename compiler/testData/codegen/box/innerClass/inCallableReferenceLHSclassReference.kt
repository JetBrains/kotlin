// LANGUAGE: +ProperSupportOfInnerClassesInCallableReferenceLHS

class ClassReference<A> {
    inner class A<K> {
        inner class DeepInner

        val refFoo = A<K>::DeepInner::class
    }
}

fun box(): String {
    val refBar = ClassReference<Int>.A<Int>::DeepInner::class
    val refFoo = ClassReference<Int>().A<Int>().refFoo
    return "OK"
}