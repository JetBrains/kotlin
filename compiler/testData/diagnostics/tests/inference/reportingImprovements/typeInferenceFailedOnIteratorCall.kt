// RUN_PIPELINE_TILL: FRONTEND
class X

operator fun <T> X.iterator(): Iterable<T> = TODO()

fun test() {
    for (i in <!ITERATOR_MISSING!>X()<!>) {
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, forLoop, funWithExtensionReceiver, functionDeclaration, localProperty,
nullableType, operator, propertyDeclaration, typeParameter */
