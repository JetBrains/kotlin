import kotlin.platform.platformStatic

<error descr="[WRONG_ANNOTATION_TARGET] This annotation is not applicable to target 'class'">platformStatic</error>
class A {
    <error descr="[WRONG_ANNOTATION_TARGET] This annotation is not applicable to target 'object'">platformStatic</error>
    companion object {
        platformStatic fun a1() {

        }
    }

    <error descr="[WRONG_ANNOTATION_TARGET] This annotation is not applicable to target 'object'">platformStatic</error>
    object A {
        platformStatic fun a2() {

        }
    }

    fun test() {
        val <warning descr="[UNUSED_VARIABLE] Variable 's' is never used">s</warning> = object {
            <error descr="[PLATFORM_STATIC_NOT_IN_OBJECT] Only functions in named objects and companion objects of classes can be annotated with 'platformStatic'">platformStatic fun a3()</error> {

            }
        }
    }

    <error descr="[PLATFORM_STATIC_NOT_IN_OBJECT] Only functions in named objects and companion objects of classes can be annotated with 'platformStatic'">platformStatic fun a4()</error> {

    }
}

<error descr="[WRONG_ANNOTATION_TARGET] This annotation is not applicable to target 'interface'">platformStatic</error>
interface B {
    companion object {
        <error descr="[PLATFORM_STATIC_NOT_IN_OBJECT] Only functions in named objects and companion objects of classes can be annotated with 'platformStatic'">platformStatic fun a1()</error> {

        }
    }

    object A {
        platformStatic fun a2() {

        }
    }

    fun test() {
        val <warning descr="[UNUSED_VARIABLE] Variable 's' is never used">s</warning> = object {
            <error descr="[PLATFORM_STATIC_NOT_IN_OBJECT] Only functions in named objects and companion objects of classes can be annotated with 'platformStatic'">platformStatic fun a3()</error> {

            }
        }
    }

    <error descr="[PLATFORM_STATIC_NOT_IN_OBJECT] Only functions in named objects and companion objects of classes can be annotated with 'platformStatic'">platformStatic fun a4()</error> {

    }
}