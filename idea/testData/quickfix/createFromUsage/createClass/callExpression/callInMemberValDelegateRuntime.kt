// "Create class 'Foo'" "true"
// ERROR: Class 'Foo' is not abstract and does not implement abstract member public abstract operator fun getValue(thisRef: A<T>, property: KProperty<*>): B defined in kotlin.properties.ReadOnlyProperty

open class B

class A<T>(val t: T) {
    val x: B by <caret>Foo(t, "")
}
