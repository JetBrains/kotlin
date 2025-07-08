// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE

class X

operator fun <T> X.component1(): T = TODO()

fun test() {
    val (y) = <!COMPONENT_OPERATOR_MISSING!>X()<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, destructuringDeclaration, funWithExtensionReceiver, functionDeclaration,
localProperty, nullableType, operator, propertyDeclaration, typeParameter */
