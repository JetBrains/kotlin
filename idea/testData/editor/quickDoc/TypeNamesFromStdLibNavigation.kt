class A {

}

fun foo(x : A) { }

fun main(args: Array<String>) {
    <caret>foo()
}

//INFO: <div class='definition'><pre><font color="808080"><i>TypeNamesFromStdLibNavigation.kt</i></font><br>public fun <b>foo</b>(
//INFO:     x: <a href="psi_element://A">A</a>
//INFO: ): Unit</pre></div></pre></div><table class='sections'><p></table>
