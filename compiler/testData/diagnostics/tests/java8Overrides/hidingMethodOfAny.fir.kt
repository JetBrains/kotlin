// RUN_PIPELINE_TILL: FRONTEND
interface IA {
    fun <!METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE, VIRTUAL_MEMBER_HIDDEN!>toString<!>(): String = "IB"

    override fun equals(other: Any?): Boolean
}
