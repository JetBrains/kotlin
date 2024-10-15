// RUN_PIPELINE_TILL: FRONTEND
interface IA {
    override fun <!METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE!>toString<!>(): String = "IA"

    override fun <!METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE!>equals<!>(other: Any?): Boolean = super.equals(other)

    override fun <!METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE!>hashCode<!>(): Int {
        return 42;
    }
}
