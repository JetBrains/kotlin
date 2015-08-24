import kotlin.jvm.jvmStatic

<error descr="[WRONG_ANNOTATION_TARGET] This annotation is not applicable to target 'class'">jvmStatic</error>
class A {
    <error descr="[WRONG_ANNOTATION_TARGET] This annotation is not applicable to target 'object'">jvmStatic</error>
    companion object {
        jvmStatic fun a1() {

        }
    }

    <error descr="[WRONG_ANNOTATION_TARGET] This annotation is not applicable to target 'object'">jvmStatic</error>
    object A {
        jvmStatic fun a2() {

        }
    }

    fun test() {
        val <warning descr="[UNUSED_VARIABLE] Variable 's' is never used">s</warning> = object {
            <error descr="[JVM_STATIC_NOT_IN_OBJECT] Only functions in named objects and companion objects of classes can be annotated with 'jvmStatic'">jvmStatic fun a3()</error> {

            }
        }
    }

    <error descr="[JVM_STATIC_NOT_IN_OBJECT] Only functions in named objects and companion objects of classes can be annotated with 'jvmStatic'">jvmStatic fun a4()</error> {

    }
}

<error descr="[WRONG_ANNOTATION_TARGET] This annotation is not applicable to target 'interface'">jvmStatic</error>
interface B {
    companion object {
        <error descr="[JVM_STATIC_NOT_IN_OBJECT] Only functions in named objects and companion objects of classes can be annotated with 'jvmStatic'">jvmStatic fun a1()</error> {

        }
    }

    object A {
        jvmStatic fun a2() {

        }
    }

    fun test() {
        val <warning descr="[UNUSED_VARIABLE] Variable 's' is never used">s</warning> = object {
            <error descr="[JVM_STATIC_NOT_IN_OBJECT] Only functions in named objects and companion objects of classes can be annotated with 'jvmStatic'">jvmStatic fun a3()</error> {

            }
        }
    }

    <error descr="[JVM_STATIC_NOT_IN_OBJECT] Only functions in named objects and companion objects of classes can be annotated with 'jvmStatic'">jvmStatic fun a4()</error> {

    }
}