// DO_NOT_CHECK_SYMBOL_RESTORE_K1

interface InterfaceA {
    fun foo()
}

sealed class SealedClassWithImplicitConstructor : InterfaceA {
    override fun foo() {

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

sealed class SealedClassWithExplicitConstructor(val constructorProperty: Int, constructorParameter: Long) {
    constructor(): this(1, 2L)
}
