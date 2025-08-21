// RUN_PIPELINE_TILL: FRONTEND
interface IA {
    fun <!VIRTUAL_MEMBER_HIDDEN!>toString<!>(): String = "IB"

    override fun equals(other: Any?): Boolean
}

/* GENERATED_FIR_TAGS: functionDeclaration, interfaceDeclaration, nullableType, operator, override, stringLiteral */
