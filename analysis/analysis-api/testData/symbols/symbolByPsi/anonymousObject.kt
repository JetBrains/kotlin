// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
class AnonymousContainer {
    val anonymousObject = object : Runnable {
        override fun run() {

        }
        val data = 123
    }
}
