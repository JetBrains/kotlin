package test

class A {
    var a by MyProperty()
}

class MyProperty<T> {
    fun get(t: T, p: PropertyMetadata): Int = 42
    fun set(t: T, p: PropertyMetadata, i: Int) {}
}