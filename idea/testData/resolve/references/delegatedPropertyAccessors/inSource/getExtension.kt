val x: Int <caret>by Foo()

class Foo

fun Foo.get(_this: Any?, p: Any?): Int = 1

// REF: (for Foo in <root>).get(Any?,Any?)

