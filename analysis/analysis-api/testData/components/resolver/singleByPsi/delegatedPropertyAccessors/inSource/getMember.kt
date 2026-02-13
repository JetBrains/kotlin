val x: Int <expr>by Foo()</expr>

class Foo {
    operator fun getValue(_this: Any?, p: Any?): Int = 1
}
