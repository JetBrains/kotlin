// !DIAGNOSTICS: -UNUSED_VARIABLE
// !LANGUAGE: +JvmStaticInInterface
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
            @JvmStatic fun a3() {

            }
        }
    }

    @JvmStatic fun a4() {

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
            @JvmStatic fun a3() {

            }
        }
    }

    @JvmStatic fun a4() {

    }
}