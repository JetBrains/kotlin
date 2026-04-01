// WITH_STDLIB

class MyClass {
    companion object {
        fun functionFromCompanion(i: Int) = Unit

        @JvmStatic
        fun staticFunctionFromCompanion() = 4
    }
}