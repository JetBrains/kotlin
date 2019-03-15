/**
 * Some documentation
 * on two lines.
 */
fun testMethod() {

}

fun test() {
    <caret>testMethod()
}

//INFO: <div class='definition'><pre><font color="808080"><i>OnMethodUsageMultiline.kt</i></font><br>public fun <b>testMethod</b>(): Unit</pre></div><div class='content'><p>Some documentation on two lines.</p></div><table class='sections'></table>
