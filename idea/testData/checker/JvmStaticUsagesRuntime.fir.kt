// RUNTIME
<error descr="[WRONG_ANNOTATION_TARGET] This annotation is not applicable to target 'class'">@JvmStatic</error>
class A {
    <error descr="[WRONG_ANNOTATION_TARGET] This annotation is not applicable to target 'companion object'">@JvmStatic</error>
    companion object {
        @JvmStatic fun a1() {

        }
    }

    <error descr="[WRONG_ANNOTATION_TARGET] This annotation is not applicable to target 'object'">@JvmStatic</error>
    object A {
        @JvmStatic fun a2() {

        }
    }

    fun test() {
        val s = object {
            @JvmStatic fun a3() {

            }
        }
    }

    @JvmStatic fun a4() {

    }
}

<error descr="[WRONG_ANNOTATION_TARGET] This annotation is not applicable to target 'interface'">@JvmStatic</error>
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
            @JvmStatic fun a3() {

            }
        }
    }

    @JvmStatic fun a4() {

    }
}
