// RUN_PIPELINE_TILL: FRONTEND
interface ILeft {
    override fun toString(): String
}

interface IRight {
    override fun toString(): String
}

interface IDiamond : ILeft, IRight {
    override fun <!METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE!>toString<!>(): String = "IDiamond"
}