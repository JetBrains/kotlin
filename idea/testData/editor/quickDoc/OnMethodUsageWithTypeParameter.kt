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

//INFO: <pre><b>public</b> <b>fun</b> &lt;T&gt; testMethod(a: Int, b: String): Unit <i>defined in</i> root package</pre><p>Some documentation</p>
//INFO: <dl><dt><b>Parameters:</b></dt><dd><code>T</code> - the type parameter</dd><dd><code>a</code> - Some int</dd><dd><code>b</code> - String</dd></dl>
