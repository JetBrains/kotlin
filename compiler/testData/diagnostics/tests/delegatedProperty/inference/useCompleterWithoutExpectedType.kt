class A {
    val a by MyProperty()

    fun test() {
        a: Int
    }
}

class MyProperty<R> {
    public fun get(thisRef: R, desc: PropertyMetadata): Int = throw Exception("$thisRef $desc")
}