// DO_NOT_CHECK_SYMBOL_RESTORE_K1

external class ClassC(val paramProp: String) {
    constructor() : this("")

    fun String.foo(regularParam: Int)

    class Nested {
        fun bar()
    }

    var baz: Int
}

external val prop: Int

external enum class EnumE { EnumA, EnumB }

external interface InterfaceI {
    fun fooI()
}