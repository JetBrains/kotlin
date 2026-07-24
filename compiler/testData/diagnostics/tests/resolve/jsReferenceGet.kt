// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80991

interface Data {
    operator fun <T : Any> get(key: Any): T?
}

fun test(data: Data) {
    data.<!NO_VALUE_FOR_PARAMETER!>get<!><String>()

    val x: Any = data
    x.<!UNRESOLVED_REFERENCE!>get<!><String>()
}

/* GENERATED_FIR_TAGS: functionDeclaration, interfaceDeclaration, nullableType, operator, typeConstraint, typeParameter */
