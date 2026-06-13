// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-3474
// WITH_STDLIB

// KT-3474: An inference problem with nullable SAM interfaces (Guava-like)
// Guava annotates parameters as @Nullable, so SAM lambdas receive nullable types
// and calling methods on them without null checks gives UNSAFE_CALL errors.

fun interface MyPredicate<T> {
    fun apply(input: T?): Boolean
}

fun interface MyFunction<F, T> {
    fun apply(from: F?): T?
}

fun <T> myFilter(collection: Collection<T>, predicate: MyPredicate<T>): Collection<T> {
    return collection.filter { predicate.apply(it) }
}

fun <F, T> myTransform(collection: Collection<F>, function: MyFunction<F, T>): Collection<T?> {
    return collection.map { function.apply(it) }
}

fun main() {
    val list = listOf("a", "bb", "acc")

    val gl = myTransform(
        myFilter(
            list,
            MyPredicate { s -> s<!UNSAFE_CALL!>.<!>startsWith("a") }
        ),
        MyFunction { s -> s<!UNSAFE_CALL!>.<!>length }
    )

    println(gl)
}

/* GENERATED_FIR_TAGS: funInterface, functionDeclaration, interfaceDeclaration, lambdaLiteral, localProperty,
nullableType, propertyDeclaration, stringLiteral, typeParameter */
