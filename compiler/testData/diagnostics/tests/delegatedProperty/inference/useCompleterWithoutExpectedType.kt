// !CHECK_TYPE

class A {
    val a by MyProperty()

    fun test() {
        checkSubtype<Int>(a)
    }
}

class MyProperty<R> {
    public fun get(thisRef: R, desc: PropertyMetadata): Int = throw Exception("$thisRef $desc")
}