/**
Some documentation

 * @param[a] Some int
 * @param[b] String
 */
fun testMethod(a: Int, b: String) {

}

fun test() {
    <caret>testMethod(1, "value")
}

//INFO: <b>internal</b> <b>fun</b> testMethod(a: Int, b: String): Unit<br/><p>Some documentation
//INFO: <dl><dt><b>Parameters:</b></dt><dd><code>a</code> - Some int</dd><dd><code>b</code> - String</dd></dl>
//INFO: </p>
