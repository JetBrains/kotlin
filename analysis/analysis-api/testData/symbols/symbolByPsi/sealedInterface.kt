// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1

sealed interface SealedInterface {
    fun function()

    fun defaultFunction() {

    }

    private fun privateDefaultFunction() {

    }

    var variable: Int

    var defaultVariable: Long
        get() = 1L
        set(value) {

        }

    private var privateDefaultVariable: Long
        get() = 1L
        set(value) {

        }

    class NestedClass

    inner class InnerClass

    object NestedObject

    interface NestedInterface

    annotation class NestedAnnotation

    typealias NestedTypeAlias = String
}
