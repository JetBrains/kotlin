val x: Int <caret>by Foo()

class Foo {
    fun getValue(_this: Any?, p: Any?): Int = 1
}

// REF: (in Foo).getValue(Any?, Any?)

