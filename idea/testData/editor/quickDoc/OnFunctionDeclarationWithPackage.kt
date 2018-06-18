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

//INFO: <div class='definition'><pre><a href="psi_element://test"><code>test</code></a> (OnFunctionDeclarationWithPackage.kt)<br><b>public</b> <b>fun</b> testFun(
//INFO:     first: String,
//INFO:     second: Int
//INFO: ): Int</pre></div><div class='content'><p>Test function</p></div><table class='sections'><tr><td valign='top' class='section'><p>Parameters</td><td><p><code>first</code> - Some<p><code>second</code> - Other</td></table>
