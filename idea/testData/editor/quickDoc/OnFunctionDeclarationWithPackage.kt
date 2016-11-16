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

//INFO: <pre><b>public</b> <b>fun</b> testFun(first: String, second: Int): Int <i>defined in</i> test</pre><p>Test function</p>
//INFO: <dl><dt><b>Parameters:</b></dt><dd><code>first</code> - Some</dd><dd><code>second</code> - Other</dd></dl>
