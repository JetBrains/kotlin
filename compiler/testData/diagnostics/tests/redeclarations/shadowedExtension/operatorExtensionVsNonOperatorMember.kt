// RUN_PIPELINE_TILL: BACKEND
interface Test {
    fun invoke()
    operator fun invoke(i: Int): Int
}

operator fun Test.invoke() {}
operator fun Test.<!EXTENSION_SHADOWED_BY_MEMBER!>invoke<!>(i: Int) = i

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, interfaceDeclaration, operator */
