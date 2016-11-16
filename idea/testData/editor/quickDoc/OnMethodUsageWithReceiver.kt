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

//INFO: <pre><b>public</b> <b>fun</b> Int.testMethod(b: String): Unit <i>defined in</i> root package</pre><p>Some documentation</p>
//INFO: <dl><dt><b>Receiver:</b></dt><dd>Some int</dd></dl>
//INFO: <dl><dt><b>Parameters:</b></dt><dd><code>b</code> - String</dd></dl>
//INFO: <dl><dt><b>Returns:</b></dt><dd>Return <a href="psi_element://a">a</a> and nothing else</dd></dl>
