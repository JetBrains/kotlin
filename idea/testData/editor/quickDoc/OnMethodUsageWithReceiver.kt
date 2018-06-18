/**
Some documentation

 * @receiver Some int
 * @param b String
 * @return Return [a] and nothing else
 */
fun Int.testMethod(b: String) {

}

fun test() {
    1.<caret>testMethod("value")
}

//INFO: <div class='definition'><pre>(OnMethodUsageWithReceiver.kt)<br><b>public</b> <b>fun</b> Int.testMethod(
//INFO:     b: String
//INFO: ): Unit</pre></div><div class='content'><p>Some documentation</p></div><table class='sections'><tr><td valign='top' class='section'><p>Receiver</td><td></td><tr><td valign='top' class='section'><p>Parameters</td><td><p><code>b</code> - String</td><tr><td valign='top' class='section'><p>Returns</td><td></td></table>
