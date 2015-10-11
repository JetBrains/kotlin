//ALLOW_AST_ACCESS
package test

class A {
    var a by MyProperty()
}

class MyProperty<T> {
    fun getValue(t: T, p: PropertyMetadata): Int = 42
    fun setValue(t: T, p: PropertyMetadata, i: Int) {}
}