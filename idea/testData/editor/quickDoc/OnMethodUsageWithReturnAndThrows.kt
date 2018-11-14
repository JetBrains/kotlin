/**
Some documentation

 * @param a Some int
 * @param b String
 * @return Return value
 * @throws IllegalArgumentException if the weather is bad
 */
fun testMethod(a: Int, b: String) {

}

fun test() {
    <caret>testMethod(1, "value")
}

//INFO: <div class='definition'><pre><font color="808080"><i>OnMethodUsageWithReturnAndThrows.kt</i></font><br>public fun <b>testMethod</b>(
//INFO:     a: Int,
//INFO:     b: String
//INFO: ): Unit</pre></div><div class='content'><p>Some documentation</p></div><table class='sections'><tr><td valign='top' class='section'><p>Params:</td><td valign='top'><p><code>a</code> - Some int<p><code>b</code> - String</td><tr><td valign='top' class='section'><p>Returns:</td><td valign='top'>Return value</td><tr><td valign='top' class='section'><p>Throws:</td><td valign='top'><p><code>IllegalArgumentException</code> - if the weather is bad</td></table>
