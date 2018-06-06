// !USE_EXPERIMENTAL: kotlin.Experimental
// !DIAGNOSTICS: -UNUSED_PARAMETER
// FILE: api.kt

package api

@Experimental(Experimental.Level.WARNING)
@Target(AnnotationTarget.CLASS)
annotation class E

@E
open class Foo(val s: String = "")

// FILE: usage.kt

import api.*

@UseExperimental(E::class)
class Klass {
    init {
        Foo()
    }
}

class Constructor {
    @UseExperimental(E::class) constructor() {
        Foo()
    }
}

@UseExperimental(E::class)
val property = Foo().s

@UseExperimental(E::class)
fun function() {
    Foo()
}

fun valueParameter(@UseExperimental(E::class) p: String = Foo().s): String {
    @UseExperimental(E::class)
    val localVariable: String = Foo().s
    return localVariable
}

var propertyAccessors: String
    @UseExperimental(E::class)
    get() = Foo().s
    @UseExperimental(E::class)
    set(value) { Foo() }

fun expression(): String {
    val s = @UseExperimental(E::class) Foo().s
    return s
}

@UseExperimental(E::class)
typealias TypeAlias = Foo
