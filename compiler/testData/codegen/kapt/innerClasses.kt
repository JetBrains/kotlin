// !LANGUAGE: +UseGetterNameForPropertyAnnotationsMethodOnJvm
// WITH_STDLIB

package test

class TopLevel {
    companion object {
        fun a() {}

        @JvmStatic
        val q = "A"
    }

    fun b() {}

    val x: String

    val y = 5

    class NestedClass {
        inner class NestedInnerClass
    }

    object InnerObject

    interface InnerInterface
}