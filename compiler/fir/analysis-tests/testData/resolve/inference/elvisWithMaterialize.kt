// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP

fun <T> materialize(): T = null!!
fun <T> materializeNullable(): T = null!!

fun foo(x: Any?): Boolean {
    val z = x == (materializeNullable() ?: materialize())
    return x == (materializeNullable() ?: materialize())
}

/* GENERATED_FIR_TAGS: checkNotNullCall, elvisExpression, equalityExpression, functionDeclaration, localProperty,
nullableType, propertyDeclaration, typeParameter */
