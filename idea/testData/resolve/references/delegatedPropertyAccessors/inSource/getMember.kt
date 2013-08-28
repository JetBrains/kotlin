val x: Int <caret>by Foo()

class Foo {
    fun get(_this: Any?, p: Any?): Int = 1
}

// REF: (in Foo).get(Any?,Any?)

