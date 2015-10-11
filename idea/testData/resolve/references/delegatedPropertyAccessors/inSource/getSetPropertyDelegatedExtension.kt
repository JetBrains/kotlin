var x: Int <caret>by Foo()

class Foo

fun Foo.getValue(_this: Any?, p: Any?): Int = 1
fun Foo.setValue(_this: Any?, p: Any?, val: Any?) {}
fun Foo.propertyDelegated(p: Any?) {}

// MULTIRESOLVE
// REF: (for Foo in <root>).getValue(Any?,Any?)
// REF: (for Foo in <root>).setValue(Any?,Any?,Any?)
// REF: (for Foo in <root>).propertyDelegated(Any?)

