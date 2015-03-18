package test

/**

 *
 *
 * Test function

 *
 * @param first Some
 * @param second Other
 */
fun <caret>testFun(first: String, second: Int) = 12

//INFO: <b>internal</b> <b>fun</b> testFun(first: String, second: Int): Int<br/><p>Test function
//INFO: <dl><dt><b>Parameters:</b></dt><dd><code>first</code> - Some</dd><dd><code>second</code> - Other</dd></dl>
//INFO: </p>
