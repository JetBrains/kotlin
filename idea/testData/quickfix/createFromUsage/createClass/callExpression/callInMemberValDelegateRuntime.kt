// "Create class 'Foo'" "true"
// ERROR: <html>Class 'Foo' must be declared abstract or implement abstract member<br/><b>public</b> <b>abstract</b> operator <b>fun</b> getValue(thisRef: A&lt;T&gt;, property: kotlin.reflect.KProperty&lt;*&gt;): B <i>defined in</i> kotlin.properties.ReadOnlyProperty</html>

open class B

class A<T>(val t: T) {
    val x: B by <caret>Foo(t, "")
}
