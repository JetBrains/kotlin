class NoDelegate()

class Bar

operator fun Bar.provideDelegate(_this: Any?, p: Any?): NoDelegate = NoDelegate()

val foo: Int <caret>by Bar()