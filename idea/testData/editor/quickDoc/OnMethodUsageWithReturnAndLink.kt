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

//INFO: <pre><b>public</b> <b>fun</b> testMethod(a: Int, b: String): Unit <i>defined in</i> root package</pre><p>Some documentation</p>
//INFO: <dl><dt><b>Parameters:</b></dt><dd><code>a</code> - Some int</dd><dd><code>b</code> - String</dd></dl>
//INFO: <dl><dt><b>Returns:</b></dt><dd>Return <a href="psi_element://a">a</a> and nothing else</dd></dl>
