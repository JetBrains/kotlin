val x: Int <caret>by Foo()

class Foo {
    operator fun getValue(_this: Any?, p: Any?): Int = 1
}
