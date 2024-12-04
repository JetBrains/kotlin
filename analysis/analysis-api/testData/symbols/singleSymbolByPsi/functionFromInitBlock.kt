// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
package pack

object KClient {
    init {
        fun local<caret>Fun(s: String): String = s
    }
}
