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

//INFO: <div class='definition'><pre><font color="808080"><i>OnMethodUsageWithReturnAndLink.kt</i></font><br>public fun <b>testMethod</b>(
//INFO:     a: Int,
//INFO:     b: String
//INFO: ): Unit</pre></div><div class='content'><p>Some documentation</p></div><table class='sections'><tr><td valign='top' class='section'><p>Params:</td><td valign='top'><p><code>a</code> - Some int<p><code>b</code> - String</td><tr><td valign='top' class='section'><p>Returns:</td><td valign='top'>Return <a href="psi_element://a">a</a> and nothing else</td></table>
