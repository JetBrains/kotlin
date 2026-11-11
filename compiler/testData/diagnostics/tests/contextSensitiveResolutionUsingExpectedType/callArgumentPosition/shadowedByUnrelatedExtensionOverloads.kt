// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-89822

sealed class Parent {
    companion object {
        val bar: Parent.MyClass get() = MyClass()
    }

    class MyClass : Parent() {
        companion object
    }
}

class Other {
    companion object {
        val bar: Other get() = Other()
    }
}

val Any.bar: String get() = ""

fun foo(a: Parent.MyClass): Int = 1
fun foo(a: Other): String = ""

fun main() {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>foo(bar)<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, getter, nestedClass, objectDeclaration,
propertyDeclaration, propertyWithExtensionReceiver, sealed */
