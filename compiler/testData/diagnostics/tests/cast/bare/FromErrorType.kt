// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

class G<T>

fun foo(p: <!UNRESOLVED_REFERENCE!>P<!>) {
    val v = <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>p<!> as <!NO_TYPE_ARGUMENTS_ON_RHS!>G?<!>
    checkSubtype<G<*>>(v!!)
}

/* GENERATED_FIR_TAGS: asExpression, checkNotNullCall, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionalType, infix, localProperty, nullableType, propertyDeclaration, starProjection, typeParameter,
typeWithExtension */
