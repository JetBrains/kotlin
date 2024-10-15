// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
interface ITop {
    fun foo() {}
}

interface ILeft : ITop

interface IRight : ITop

interface IDerived : ILeft, IRight