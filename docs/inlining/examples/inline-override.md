# Overriding with inline function

While inline function can't be open, it can override function from superclass.

```kotlin
// lib
interface I {
    fun foo()
}

class A : I {
    override inline fun foo() {
        println("lib.v1") // changed to lib.v2 in v2
    }
}

// depends on lib.v1

fun test(x: A) {
    x.foo() // print("lib.v1") as it is inlined
    (x as A).foo() // println("lib.v2") as it can't be inlined
}

// main depends on lib.v2
fun main() {
    test(A())
}
```

This leads to inconsistency on which version would be called in which cases. 
This code already emits a warning (or error if there is reified type paramter), 
so we are fine with this behaviour.

On the other side, there is a trickier case with the same effect, which doesn't emit any warnings.
Probably, it is a bug, and this should be deprecated ([KT-63928](https://youtrack.jetbrains.com/issue/KT-63928)).

```kotlin
interface Foo {
    fun <T> foo()
}

open class Bar {
    inline fun <reified T> foo() {
        println(typeOf<T>())
    }
}

class Bas: Foo, Bar()

fun main() {
    Bas().foo<String>() // runtime crash
}
```

This leads to a restriction: inline functions should persist as normal ones after inlining, if they can be called. 
And probably, ones, that can't be called should still exist with throw exception as body. 