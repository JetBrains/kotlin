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

//INFO: <div class='definition'><pre>(OnMethodUsageWithMultilineParam.kt)<br><b>public</b> <b>fun</b> testMethod(
//INFO:     test: String
//INFO: ): Unit</pre></div><div class='content'><p>Some documentation on two lines.</p></div><table class='sections'><tr><td valign='top' class='section'><p>Parameters</td><td><p><code>test</code> - String on two lines</td></table>
