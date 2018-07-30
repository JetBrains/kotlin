
interface Foo

fun foo(a: Any) {}

fun Foo.bar() {
    foo(this<caret>)
}

//INFO: <div class='definition'><pre><font color="808080"><i>ExtensionReceiverEnd.kt</i></font><br>public fun <a href="psi_element://Foo">Foo</a>.<b>bar</b>(): Unit</pre></div></pre></div><table class='sections'><p></table>
