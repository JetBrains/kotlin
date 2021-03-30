// RUNTIME
@JvmStatic
class A {
    @JvmStatic
    companion object {
        @JvmStatic fun a1() {

        }
    }

    @JvmStatic
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

@JvmStatic
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
