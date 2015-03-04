var x: Int <caret>by Foo()

class Foo

fun Foo.get(_this: Any?, p: Any?): Int = 1
fun Foo.set(_this: Any?, p: Any?, val: Any?) {}
fun Foo.propertyDelegated(p: Any?) {}

// MULTIRESOLVE
// REF: (for Foo in <root>).get(Any?,Any?)
// REF: (for Foo in <root>).set(Any?,Any?,Any?)
// REF: (for Foo in <root>).propertyDelegated(Any?)

