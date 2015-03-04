var x: Int <caret>by Foo()

class Foo {
    fun get(_this: Any?, p: Any?): Int = 1
    fun set(_this: Any?, p: Any?, val: Any?) {}
    fun propertyDelegated(p: Any?)
}

// MULTIRESOLVE
// REF: (in Foo).get(Any?,Any?)
// REF: (in Foo).set(Any?,Any?,Any?)
// REF: (in Foo).propertyDelegated(Any?)

