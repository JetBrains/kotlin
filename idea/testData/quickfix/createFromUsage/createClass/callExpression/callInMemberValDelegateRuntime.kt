// "Create class 'Foo'" "true"
// ERROR: <html>Class 'Foo' must be declared abstract or implement abstract member<br/><b>public</b> <b>abstract</b> <b>fun</b> get(thisRef: A&lt;T&gt;, property: kotlin.PropertyMetadata): B <i>defined in</i> kotlin.properties.ReadOnlyProperty</html>

open class B

class A<T>(val t: T) {
    val x: B by <caret>Foo(t, "")
}
