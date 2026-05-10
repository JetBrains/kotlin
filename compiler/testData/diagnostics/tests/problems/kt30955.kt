// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-30955

// KT-30955: Incorrect inference of type parameter from constraint with intersection types with constraint errors

interface A
class Inv<T>
fun <U> Inv<U>.ext2(t: U): U = TODO()

fun <T> bar(t: T, f: Inv<T>) {
    f <!UNCHECKED_CAST!>as Inv<A><!>
    val err = f.ext2(t)
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, funWithExtensionReceiver, functionDeclaration,
interfaceDeclaration, intersectionType, localProperty, nullableType, propertyDeclaration, smartcast, typeParameter */
