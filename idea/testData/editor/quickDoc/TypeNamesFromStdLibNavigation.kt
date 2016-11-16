class A {

}

fun foo(x : A) { }

fun main(args: Array<String>) {
    <caret>foo()
}

//INFO: <pre><b>public</b> <b>fun</b> foo(x: <a href="psi_element://A">A</a>): Unit <i>defined in</i> root package</pre>
