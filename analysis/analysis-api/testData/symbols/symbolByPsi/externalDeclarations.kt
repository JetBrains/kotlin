// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
// DO_NOT_CHECK_SYMBOL_RESTORE_K1

external class ClassC(val paramProp: String) {
    constructor() : this("")

    init {

    }

    fun String.foo(regularParam: Int) {
        val local = 0
    }

    class Nested<T> {
        fun bar()
    }

    var baz: Int
}

external val prop: Int

external enum class EnumE { EnumA, EnumB }

enum class EnumNotExternal { external EnumA2 }

class NotExternalC(external val paramProp2: String) {
    external constructor() : this("")
}

external interface InterfaceI {
    fun fooI()
}

sealed external class SealedC {
    data object SealedO : SealedC()
}

val propWithExternalGetter: Int
    external get

var propWithExternalAccessors: Int
    external get
    external set
