// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-54400
// CHECK_TYPE_WITH_EXACT

fun test() {
    val buildee = build {
        typeVariableMutableProperty = ""
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<String>>(buildee)
}




class Buildee<TV> {
    var typeVariableMutableProperty: TV
        get() = storage
        set(value) { storage = value }
    private var storage: TV = null!!
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}

/* GENERATED_FIR_TAGS: assignment, checkNotNullCall, classDeclaration, functionDeclaration, functionalType, getter,
lambdaLiteral, localProperty, nullableType, propertyDeclaration, setter, stringLiteral, typeParameter, typeWithExtension */
