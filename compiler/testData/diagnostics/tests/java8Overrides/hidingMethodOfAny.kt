// RUN_PIPELINE_TILL: SOURCE
interface IA {
    fun <!VIRTUAL_MEMBER_HIDDEN!>toString<!>(): String = "IB"

    override fun equals(other: Any?): Boolean
}