val x: Int <expr>by Foo()</expr>

class Foo

operator fun Foo.getValue(_this: Any?, p: Any?): Int = 1

