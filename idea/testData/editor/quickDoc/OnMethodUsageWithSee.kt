/**
 * @see C
 * @see D
 * @see <a href="http://kotl.in">kotlin</a>
 */
fun testMethod() {

}

class C {
}

class D {
}

fun test() {
    <caret>testMethod(1, "value")
}

//INFO: <pre><b>public</b> <b>fun</b> testMethod(): Unit <i>defined in</i> root package</pre><br/>
//INFO: <DD><DL><DT><b>See Also:</b><DD><a href="psi_element://C"><code>C</code></a>, <a href="psi_element://D"><code>D</code></a>, <a href="http://kotl.in">kotlin</a></DD></DL></DD>
