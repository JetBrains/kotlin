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

//INFO: <div class='definition'><pre>(OnMethodUsageWithTypeParameter.kt)<br><b>public</b> <b>fun</b> &lt;T&gt; testMethod(
//INFO:     a: Int,
//INFO:     b: String
//INFO: ): Unit</pre></div><div class='content'><p>Some documentation</p></div><table class='sections'><tr><td valign='top' class='section'><p>Parameters</td><td><p><code>T</code> - the type parameter<p><code>a</code> - Some int<p><code>b</code> - String</td></table>
