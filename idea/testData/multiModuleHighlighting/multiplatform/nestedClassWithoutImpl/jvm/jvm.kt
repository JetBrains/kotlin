package a

actual class <error>A</error>

actual class B {
    actual class <error>Nested</error>
}

actual class C {
    actual inner class Inner
}
