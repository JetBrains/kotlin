
interface Foo

fun foo(a: Any) {}

fun Foo.bar() {
    foo(th<caret>is)
}

//INFO: <div class='definition'><pre>(ExtensionReceiver.kt)<br><b>public</b> <b>fun</b> <a href="psi_element://Foo">Foo</a>.bar(): Unit</pre></div></pre></div><table class='sections'><p></table>
