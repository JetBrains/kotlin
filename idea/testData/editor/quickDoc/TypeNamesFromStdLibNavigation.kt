class A {

}

fun foo(x : A) { }

fun main(args: Array<String>) {
    <caret>foo()
}

//INFO: <div class='definition'><pre>(TypeNamesFromStdLibNavigation.kt)<br><b>public</b> <b>fun</b> foo(
//INFO:     x: <a href="psi_element://A">A</a>
//INFO: ): Unit</pre></div></pre></div><table class='sections'><p></table>
