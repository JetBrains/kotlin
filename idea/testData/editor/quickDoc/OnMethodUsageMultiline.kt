/**
 * Some documentation
 * on two lines.
 */
fun testMethod() {

}

fun test() {
    <caret>testMethod()
}

//INFO: <pre><b>public</b> <b>fun</b> testMethod(): Unit <i>defined in</i> root package</pre><p>Some documentation on two lines.</p>
