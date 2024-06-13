// MODULE: lib

// FILE: some/Foo.kt
package some

class Foo

class FooWithGeneric<T>(
    val value: T? = null,
    val flag: Boolean = false,
) {
    constructor(p: Any) : this(p as? T, true)
}

// MODULE: app(lib)
// MODULE_KIND: Source
// FILE: main.kt

import some.*

fun test() {
    val x = Foo()
    val y = FooWithGeneric<Int>(42)
    val z = FooWith<caret>Generic<String>(42)
}