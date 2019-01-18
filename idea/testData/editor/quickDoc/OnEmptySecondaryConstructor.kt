/**
Documentation is here.
 */
class Foo {
    private var x: Int = 0

    constructor(x: Int) {
        this.x = x
    } //secondary constructor without documentation
}

fun test() {
    val f = Foo<caret>(10)
}
//INFO: <div class='definition'><pre><a href="psi_element://Foo"><code>Foo</code></a><br>public constructor <b>Foo</b>(
//INFO:     x: Int
//INFO: )</pre></div><div class='content'><p>Documentation is here.</p></div><table class='sections'></table>
