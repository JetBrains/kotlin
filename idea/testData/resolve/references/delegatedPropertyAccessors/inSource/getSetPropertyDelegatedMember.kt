var x: Int <caret>by Foo()

class Foo {
    fun getValue(_this: Any?, p: Any?): Int = 1
    fun setValue(_this: Any?, p: Any?, val: Any?) {}
    fun propertyDelegated(p: Any?)
}

// MULTIRESOLVE
// REF: (in Foo).getValue(Any?,Any?)
// REF: (in Foo).setValue(Any?,Any?,Any?)
// REF: (in Foo).propertyDelegated(Any?)

