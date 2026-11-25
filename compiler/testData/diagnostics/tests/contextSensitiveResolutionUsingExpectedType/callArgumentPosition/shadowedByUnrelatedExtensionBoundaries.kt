// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89822
// FILE: noCompanionInSubclass.kt
package noCompanionInSubclass

sealed class Parent {
    companion object {
        val bar: Parent.MyClass get() = MyClass()
    }

    class MyClass : Parent()
}

val Any.bar: String get() = ""

fun foo(a: Parent.MyClass) {}

fun main() {
    foo(bar)
}

// FILE: noCompanionInMiddleSealedLevel.kt
package noCompanionInMiddleSealedLevel

sealed class G {
    companion object {
        val bar: G.P.MyClass get() = P.MyClass()
    }

    sealed class P : G() {
        class MyClass : P()
    }
}

val Any.bar: String get() = ""

fun foo(a: G.P.MyClass) {}

fun main() {
    foo(bar)
}

// FILE: expectedTypeIsSealedParent.kt
package expectedTypeIsSealedParent

sealed class Parent {
    companion object {
        val bar: Parent.MyClass get() = MyClass()
    }

    class MyClass : Parent() {
        companion object
    }
}

val Any.bar: String get() = ""

fun main() {
    val s: Parent = bar
}

// FILE: memberWins.kt
package memberWins

class MyClass {
    companion object {
        val bar: MyClass get() = MyClass()
    }
}

enum class E {
    bar;

    companion object
}

open class WithObject {
    companion object

    object bar : WithObject()
}

val Any.bar: String get() = ""

fun foo(a: MyClass) {}
fun fooE(e: E) {}
fun fooWithObject(w: WithObject) {}

fun main() {
    foo(bar)
    fooE(bar)
    fooWithObject(bar)
}

// FILE: regularResolution.kt
package regularResolution

sealed class Parent {
    companion object {
        val bar: Parent.MyClass get() = MyClass()
    }

    class MyClass : Parent() {
        companion object
    }
}

val Any.bar: String get() = ""

fun foo(a: Parent.MyClass) {}

fun main() {
    foo(<!ARGUMENT_TYPE_MISMATCH!>Parent.MyClass.bar<!>)
}

class C {
    fun test() = foo(<!ARGUMENT_TYPE_MISMATCH!>bar<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, enumDeclaration, enumEntry, functionDeclaration, getter,
localProperty, nestedClass, objectDeclaration, propertyDeclaration, propertyWithExtensionReceiver, sealed */
