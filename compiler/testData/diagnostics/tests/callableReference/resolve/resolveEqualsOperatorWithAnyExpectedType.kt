// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL

interface Base

fun <K> materialize(): K = TODO()

fun <T : Base> Base.transform(): T = materialize()

fun test(child: Base) {
    child == child.transform()
}

/* GENERATED_FIR_TAGS: equalityExpression, funWithExtensionReceiver, functionDeclaration, interfaceDeclaration,
nullableType, typeConstraint, typeParameter */
