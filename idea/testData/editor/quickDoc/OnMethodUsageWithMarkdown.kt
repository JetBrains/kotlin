/**
 * Some documentation. **Bold** *underline* `code` foo: bar (baz) [quux] <xyzzy> 'apos'
 *
 * [Kotlin](http://www.kotlinlang.org)
 * [a**b**__d__ kas ](http://www.ibm.com)
 *
 * [C]
 *
 * [See **this** class][C]
 *
 * This is _emphasized text_ but text_with_underscores has to preserve the underscores.
 * Single stars embedded in a word like Embedded*Star have to be preserved as well.
 *
 * Exclamation marks are also important! Also in `code blocks!`
 *
 * bt+ : ``prefix ` postfix``
 * backslash: `\`
 */
fun testMethod() {

}

class C {
}

fun test() {
    <caret>testMethod(1, "value")
}

//INFO: <b>public</b> <b>fun</b> testMethod(): Unit <i>defined in</i> root package<p>Some documentation. <strong>Bold</strong> <em>underline</em> <code>code</code> foo: bar (baz) <a href="psi_element://quux">quux</a>  'apos'</p>
//INFO: <p><a href="http://www.kotlinlang.org">Kotlin</a> <a href="http://www.ibm.com">a<strong>b</strong><strong>d</strong> kas</a></p>
//INFO: <p><a href="psi_element://C">C</a></p>
//INFO: <p><a href="psi_element://C">See <strong>this</strong> class</a></p>
//INFO: <p>This is <em>emphasized text</em> but text_with_underscores has to preserve the underscores. Single stars embedded in a word like Embedded*Star have to be preserved as well.</p>
//INFO: <p>Exclamation marks are also important! Also in <code>code blocks!</code></p>
//INFO: <p>bt+ : <code>prefix ` postfix</code> backslash: <code>\</code></p>
