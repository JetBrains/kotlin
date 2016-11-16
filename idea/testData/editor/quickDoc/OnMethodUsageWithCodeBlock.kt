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

//INFO: <pre><b>public</b> <b>fun</b> testMethod(): Unit <i>defined in</i> root package</pre><p>Some documentation.</p>
//INFO: <pre><code>
//INFO: Code block
//INFO:     Second line
//INFO:
//INFO: Third line
//INFO: </code></pre><p>Text between code blocks.</p>
//INFO: <pre><code>
//INFO: </code></pre><p>Text after code block.</p>
