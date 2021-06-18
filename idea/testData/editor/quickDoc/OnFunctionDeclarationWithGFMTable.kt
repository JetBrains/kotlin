package test

/**
 * Test function
 *
 * | foo  | bar   | qux |
 * | ---: | :---: | --- |
 * | baz  | bim   | box |
 *
 * @param first Some
 * @param second Other
 */
fun <caret>testFun(first: String, second: Int) = 12

//INFO: <div class='definition'><pre><a href="psi_element://test"><code>test</code></a> <font color="808080"><i>OnFunctionDeclarationWithGFMTable.kt</i></font><br>public fun <b>testFun</b>(
//INFO:     first: String,
//INFO:     second: Int
//INFO: ): Int</pre></div><div class='content'><p>Test function</p>
//INFO:   <table><thead><tr><th align="right"> foo</th><th align="center"> bar</th><th align="left"> qux</th></tr></thead><tbody><tr><td align="right"> baz</td><td align="center"> bim</td><td align="left"> box</td></tr></tbody></table></div><table class='sections'><tr><td valign='top' class='section'><p>Params:</td><td valign='top'><p><code>first</code> - Some<p><code>second</code> - Other</td></table>
