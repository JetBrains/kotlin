// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE
class A {
    companion object {
        @JvmStatic fun a1() {

        }
    }

    object A {
        @JvmStatic fun a2() {

        }
    }

    fun test() {
        val s = object {
            <!JVM_STATIC_NOT_IN_OBJECT_OR_COMPANION!>@JvmStatic fun a3()<!> {

            }
        }
    }

    <!JVM_STATIC_NOT_IN_OBJECT_OR_COMPANION!>@JvmStatic fun a4()<!> {

    }
}

interface B {
    companion object {
        @JvmStatic fun a1() {

        }
    }

    object A {
        @JvmStatic fun a2() {

        }
    }

    fun test() {
        val s = object {
            <!JVM_STATIC_NOT_IN_OBJECT_OR_COMPANION!>@JvmStatic fun a3()<!> {

            }
        }
    }

    <!JVM_STATIC_NOT_IN_OBJECT_OR_COMPANION!>@JvmStatic fun a4()<!> {

    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, companionObject, functionDeclaration,
interfaceDeclaration, localProperty, nestedClass, objectDeclaration, propertyDeclaration */
