// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1

interface InterfaceA {
    fun foo()
}

object Object : InterfaceA {
    override fun foo() {
    }

    constructor() {

    }

    fun function() {

    }

    private fun privateFunction() {

    }

    internal fun internalFunction() {

    }

    var variable = 1

    private var privateVariable = 1

    internal var internalVariable = 1

    class NestedClass

    inner class InnerClass

    object NestedObject

    interface NestedInterface

    annotation class NestedAnnotation

    typealias NestedTypeAlias = String
}
