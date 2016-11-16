/**
Some documentation

 * @param a Some int
 * @param b String
 */
fun testMethod(a: Int, b: String) {

}

fun test() {
    <caret>testMethod(1, "value")
}

//INFO: <pre><b>public</b> <b>fun</b> testMethod(a: Int, b: String): Unit <i>defined in</i> root package</pre><p>Some documentation</p>
//INFO: <dl><dt><b>Parameters:</b></dt><dd><code>a</code> - Some int</dd><dd><code>b</code> - String</dd></dl>
