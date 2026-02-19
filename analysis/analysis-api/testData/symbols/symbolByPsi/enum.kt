// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
// DO_NOT_CHECK_SYMBOL_RESTORE_K1

enum class Regular {
    Entry1,
    EntryWithBody {
        fun function2() {

        }

        var variable2 = 1

        private fun privateFunction2() {

        }

        internal fun internalFunction2() {

        }

        private var privateVariable2 = 2

        internal var internalVariable2 = 3

        inner class InnerClass
    };

    fun function() {

    }

    private fun privateFunction() {

    }

    internal fun internalFunction() {

    }

    var variable = 4

    private var privateVariable = 5

    internal var internalVariable = 6

    class NestedClass

    inner class InnerClass

    object NestedObject

    interface NestedInterface

    annotation class NestedAnnotation

    typealias NestedTypeAlias = String
}

enum class WithConstructor(val property: Int, parameter: Int) {
    Entry(1, 2),
    WithBody(3, 4) {

    }
}
