// A

class A {
    companion object {
        @JvmName("realName")
        fun f1() {

        }

        @JvmName(1)
        fun f2() {}

        @JvmStatic(3)
        fun f3() {

        }

        @JvmName
        fun f4() {

        }

        @JvmOverloads(1, 2, 3, 4)
        fun f5() {

        }

        @JvmOverloads("arg")
        fun f6() {

        }
    }
}