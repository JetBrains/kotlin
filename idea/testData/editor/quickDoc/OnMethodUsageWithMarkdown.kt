/**
 * Some documentation. **Bold** *underline* `code` foo: bar (baz) [quux] <xyzzy> 'apos'
 *
 * [Kotlin](http://www.kotlinlang.org)
 *
 * [C]
 *
 * [See this class][C]
 */
fun testMethod() {

}

class C {
}

fun test() {
    <caret>testMethod(1, "value")
}

//INFO: <b>internal</b> <b>fun</b> testMethod(): Unit <i>defined in</i> root package<p>Some documentation. <strong>Bold</strong> <em>underline</em> <code>code</code> foo: bar (baz) <a href="psi_element://quux">quux</a>  'apos'</p>
//INFO: <p><a href="http://www.kotlinlang.org">Kotlin</a></p>
//INFO: <p><a href="psi_element://C">C</a></p>
//INFO: <p><a href="psi_element://C">See this class</a></p>
