// RUN_PIPELINE_TILL: FRONTEND
interface IA

interface IB : IA {
    override fun <!METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE!>toString<!>(): String = "IB"
}

interface IC : IB {
    override fun <!METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE!>toString<!>(): String = "IC"
}

interface ID : IC {
    override fun <!METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE!>toString<!>(): String = "ID"
}