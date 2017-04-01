
interface Foo

fun foo(a: Any) {}

fun Foo.bar() {
    foo(th<caret>is)
}

//INFO: <pre><b>public</b> <b>fun</b> <a href="psi_element://Foo">Foo</a>.bar(): Unit <i>defined in</i> root package</pre>
