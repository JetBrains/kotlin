class Foo

operator fun Foo.getValue(_this: Any?, p: Any?): Int = 1

class Bar

operator fun Bar.provideDelegate(_this: Any?, p: Any?): Foo = Foo()

val x: Int <caret>by Bar()

