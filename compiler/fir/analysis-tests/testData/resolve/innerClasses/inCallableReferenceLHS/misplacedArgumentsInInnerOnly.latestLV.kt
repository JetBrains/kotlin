// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-82122
// LATEST_LV_DIFFERENCE
// RENDER_DIAGNOSTICS_FULL_TEXT

open class X<A> {
    inner class Y<B> {
        fun foo() {
        }
    }

    private val ref = Y<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!>::foo

    class Z {
        val ref = Y<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!>::foo
    }
}

class T : X<String>() {
    val ref = Y<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!>::foo
}

fun <C> foo() {
    class X<A> {
        inner class Y<B> {
            fun foo() {
            }
        }

        val ref = Y<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!>::foo
    }
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, inner, localClass, nestedClass,
nullableType, propertyDeclaration, typeParameter */
