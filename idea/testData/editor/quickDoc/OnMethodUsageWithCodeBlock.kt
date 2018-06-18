/**
 * Some documentation.
 *
 * ```
 * Code block
 *     Second line
 *
 * Third line
 * ```
 *
 * Text between code blocks.
 * ```
 * ```
 * Text after code block.
 */
fun testMethod() {

}

class C {
}

fun test() {
    <caret>testMethod(1, "value")
}

//INFO: <div class='definition'><pre>(OnMethodUsageWithCodeBlock.kt)<br><b>public</b> <b>fun</b> testMethod(): Unit</pre></div><div class='content'><p>Some documentation.</p>
//INFO: <pre><code>
//INFO: Code block
//INFO:     Second line
//INFO:
//INFO: Third line
//INFO: </code></pre><p>Text between code blocks.</p>
//INFO: <pre><code>
//INFO: </code></pre><p>Text after code block.</p></div><table class='sections'></table>
