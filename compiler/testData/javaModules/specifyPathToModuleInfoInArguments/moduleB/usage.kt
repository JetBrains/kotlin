import foo.Foo
import unexported.Unexported

fun usage() {
    // Should be OK
    Foo()

    // Should be error
    Unexported()

    // Should be error: if we're passing the path to a particular module-info.java to the compiler,
    // we should not treat its location as a root
    Unrelated()
}
