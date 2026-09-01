// RUN_PIPELINE_TILL: BACKEND
interface T1 {
    fun getX() = 1
}

interface T2 {
    val x: Int
        get() = 1
}

class <!CONFLICTING_INHERITED_JVM_DECLARATIONS!>C<!> : T1, T2 {
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, getter, integerLiteral, interfaceDeclaration,
propertyDeclaration */
