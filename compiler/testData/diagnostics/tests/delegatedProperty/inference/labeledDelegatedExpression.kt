class A3 {
    val a: String by @l MyProperty()

    class MyProperty<T> {}

    fun <T> MyProperty<T>.get(thisRef: Any?, desc: PropertyMetadata): T {
        throw Exception("$thisRef $desc")
    }
}