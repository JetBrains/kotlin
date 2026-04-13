import foo.*
import bar.*

fun test(): String {
    val f = Foo()
    val b = Bar.getFoo()
    return "$f$b"
}
