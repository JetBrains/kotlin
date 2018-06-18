/**
Some documentation

 * @param a Some int
 * @param b String
 * @return Return [a] and nothing else
 */
fun testMethod(a: Int, b: String) {

}

fun test() {
    <caret>testMethod(1, "value")
}

//INFO: <div class='definition'><pre>(OnMethodUsageWithReturnAndLink.kt)<br><b>public</b> <b>fun</b> testMethod(
//INFO:     a: Int,
//INFO:     b: String
//INFO: ): Unit</pre></div><div class='content'><p>Some documentation</p></div><table class='sections'><tr><td valign='top' class='section'><p>Parameters</td><td><p><code>a</code> - Some int<p><code>b</code> - String</td><tr><td valign='top' class='section'><p>Returns</td><td></td></table>
