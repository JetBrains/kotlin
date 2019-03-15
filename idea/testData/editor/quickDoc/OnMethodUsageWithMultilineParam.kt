/**
 * Some documentation
 * on two lines.
 *
 * @param test String
 * on two lines
 */
fun testMethod(test: String) {
}

fun test() {
    <caret>testMethod("")
}

//INFO: <div class='definition'><pre><font color="808080"><i>OnMethodUsageWithMultilineParam.kt</i></font><br>public fun <b>testMethod</b>(
//INFO:     test: String
//INFO: ): Unit</pre></div><div class='content'><p>Some documentation on two lines.</p></div><table class='sections'><tr><td valign='top' class='section'><p>Params:</td><td valign='top'><p><code>test</code> - String on two lines</td></table>
