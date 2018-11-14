/**
Some documentation

 * @param T the type parameter
 * @param a Some int
 * @param b String
 */
fun <T> testMethod(a: Int, b: String) {

}

fun test() {
    <caret>testMethod(1, "value")
}

//INFO: <div class='definition'><pre><font color="808080"><i>OnMethodUsageWithTypeParameter.kt</i></font><br>public fun &lt;T&gt; <b>testMethod</b>(
//INFO:     a: Int,
//INFO:     b: String
//INFO: ): Unit</pre></div><div class='content'><p>Some documentation</p></div><table class='sections'><tr><td valign='top' class='section'><p>Params:</td><td valign='top'><p><code>T</code> - the type parameter<p><code>a</code> - Some int<p><code>b</code> - String</td></table>
