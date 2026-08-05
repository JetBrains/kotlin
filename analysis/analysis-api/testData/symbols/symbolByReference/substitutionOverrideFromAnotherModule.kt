// MODULE: dep
// FILE: base.kt
package test

class Foo

abstract class Base<T> {
    fun withOuterGeneric(t: T): String = "str"
}

// FILE: derived.kt
package test

class ClassWithGenericBase : Base<Foo>()

// MODULE: main(dep)
// FILE: main.kt
package test

fun usage(c: ClassWithGenericBase, foo: Foo) {
    c.with<caret>OuterGeneric(foo)
}
