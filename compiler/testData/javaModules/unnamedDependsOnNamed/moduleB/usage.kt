import foo.Foo
import foo.impl.Impl

fun usage() {
    Foo()  // should be OK

    Impl()  // should be error
}
