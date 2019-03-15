/**
 * Some documentation. **Bold** *underline* `code` foo: bar (baz) [quux] <xyzzy> 'apos'
 *
 * [Kotlin](https://www.kotlinlang.org)
 * [a**b**__d__ kas ](https://www.ibm.com)
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

//INFO: <div class='definition'><pre><font color="808080"><i>OnMethodUsageWithMarkdown.kt</i></font><br>public fun <b>testMethod</b>(): Unit</pre></div><div class='content'><p>Some documentation. <strong>Bold</strong> <em>underline</em> <code>code</code> foo: bar (baz) <a href="psi_element://quux">quux</a>  'apos'</p>
//INFO: <p><a href="https://www.kotlinlang.org">Kotlin</a> <a href="https://www.ibm.com">a<strong>b</strong><strong>d</strong> kas</a></p>
//INFO: <p><a href="psi_element://C">C</a></p>
//INFO: <p><a href="psi_element://C">See <strong>this</strong> class</a></p>
//INFO: <p>This is <em>emphasized text</em> but text_with_underscores has to preserve the underscores. Single stars embedded in a word like Embedded*Star have to be preserved as well.</p>
//INFO: <p>Exclamation marks are also important! Also in <code>code blocks!</code></p>
//INFO: <p>bt+ : <code>prefix ` postfix</code> backslash: <code>\</code></p></div><table class='sections'></table>
