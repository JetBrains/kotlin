val x: Int <caret>by Foo()

class Foo

operator fun Foo.getValue(_this: Any?, p: Any?): Int = 1

