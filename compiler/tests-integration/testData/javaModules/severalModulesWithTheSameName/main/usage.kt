import foo.Foo
import bar.Bar

fun usage(): String {
    val f: Foo = Foo()
    val b: Bar = Bar()
    return "$f$b"
}
