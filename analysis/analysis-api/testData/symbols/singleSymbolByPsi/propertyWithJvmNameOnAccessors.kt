// WITH_STDLIB

class Foo {
    var <caret>x: Int = 42
        @JvmName("acquireX") get
        @JvmName("changeX") set
}
