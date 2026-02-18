// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
class AnonymousContainer {
    val anonymousObject = object : Runnable {
        override fun run() {

        }

        val data = 123

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

        constructor() {

        }
    }
}
