// !DIAGNOSTICS: -UNUSED_VARIABLE
import kotlin.jvm.jvmStatic

class A {
    companion object {
        jvmStatic fun a1() {

        }
    }

    object A {
        jvmStatic fun a2() {

        }
    }

    fun test() {
        val s = object {
            <!JVM_STATIC_NOT_IN_OBJECT!>jvmStatic fun a3()<!> {

            }
        }
    }

    <!JVM_STATIC_NOT_IN_OBJECT!>jvmStatic fun a4()<!> {

    }
}

interface B {
    companion object {
        <!JVM_STATIC_NOT_IN_OBJECT!>jvmStatic fun a1()<!> {

        }
    }

    object A {
        jvmStatic fun a2() {

        }
    }

    fun test() {
        val s = object {
            <!JVM_STATIC_NOT_IN_OBJECT!>jvmStatic fun a3()<!> {

            }
        }
    }

    <!JVM_STATIC_NOT_IN_OBJECT!>jvmStatic fun a4()<!> {

    }
}