/**
 * Some documentation.
 *
 * ```
 * Code block
 *     Second line
 *
 * Third line
 * ```
 */
fun testMethod() {

}

class C {
}

fun test() {
    <caret>testMethod(1, "value")
}

//INFO: <b>public</b> <b>fun</b> testMethod(): Unit <i>defined in</i> root package<p>Some documentation.</p>
//INFO: <pre><code>
//INFO: Code block
//INFO:     Second line
//INFO:
//INFO: Third line
//INFO: </code><pre>
