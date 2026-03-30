// RUN_PIPELINE_TILL: BACKEND
interface ITop {
    fun foo() {}
}

interface ILeft : ITop

interface IRight : ITop

interface IDerived : ILeft, IRight

/* GENERATED_FIR_TAGS: functionDeclaration, interfaceDeclaration */
