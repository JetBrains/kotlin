// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-53478

class UncompilingClass<T : Any>(
    val block: (UncompilingClass<T>.() -> Unit)? = null,
) {

    var uncompilingFun: ((T) -> Unit)? = null
}

fun handleInt(arg: Int) = Unit

fun box() {
    val obj = UncompilingClass {
        uncompilingFun = { handleInt(it) }
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, functionalType, lambdaLiteral, localProperty,
nullableType, primaryConstructor, propertyDeclaration, typeConstraint, typeParameter, typeWithExtension */
