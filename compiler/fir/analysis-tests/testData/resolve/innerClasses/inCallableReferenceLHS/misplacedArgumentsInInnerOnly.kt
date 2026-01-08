// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-82122

open class X<A> {
    inner class Y<B> {
        fun foo() {
        }
    }

    private val ref = Y<Int, Int>::foo

    class Z {
        val ref = Y<Int, Int>::foo
    }
}

class T : X<String>() {
    val ref = Y<Int, Int>::foo
}

fun <C> foo() {
    class X<A> {
        inner class Y<B> {
            fun foo() {
            }
        }

        val ref = Y<Int, Int>::foo
    }
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, inner, localClass, nestedClass,
nullableType, propertyDeclaration, typeParameter */
