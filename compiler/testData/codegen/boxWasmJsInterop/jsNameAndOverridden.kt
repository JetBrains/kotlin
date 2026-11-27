// FILE: jsNameAndOverriddenExternal.js
class Super {
    foo() { return 23; }
}

// The JS subclass defines its own `foo`, which shadows Super.prototype.foo
class Sub extends Super {
    foo() { return 42; }
}

// The JS subclass doesn't define `foo`, so it inherits Super.prototype.foo
class SubNoOwnFoo extends Super {}

// FILE: jsNameAndOverriddenExternal.kt
open external class Super {
    fun foo(): Int
}

external class Sub : Super {
    @JsName("foo") fun bar(): Int
}

external class SubNoOwnFoo : Super {
    @JsName("foo") fun bar(): Int
}

fun box(): String {
    val sup = Super()
    if (sup.foo() != 23) return "Fail 1: ${sup.foo()}"

    val sub = Sub()
    if (sub.bar() != 42) return "Fail 2: ${sub.bar()}"
    // Kotlin-final Super.foo still goes through JS dynamic dispatch, so it reaches Sub's `foo`
    if (sub.foo() != 42) return "Fail 3: ${sub.foo()}"
    val subAsSuper: Super = sub
    if (subAsSuper.foo() != 42) return "Fail 4: ${subAsSuper.foo()}"

    val sub2 = SubNoOwnFoo()
    if (sub2.bar() != 23) return "Fail 5: ${sub2.bar()}"
    if (sub2.foo() != 23) return "Fail 6: ${sub2.foo()}"

    return "OK"
}
