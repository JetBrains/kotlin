
interface Foo

fun foo(a: Any) {}

fun Foo.bar() {
    foo(this<caret>)
}

//INFO: <pre><b>public</b> <b>fun</b> <a href="psi_element://Foo">Foo</a>.bar(): Unit <i>defined in</i> root package <i>in file</i> ExtensionReceiverEnd.kt</pre>
