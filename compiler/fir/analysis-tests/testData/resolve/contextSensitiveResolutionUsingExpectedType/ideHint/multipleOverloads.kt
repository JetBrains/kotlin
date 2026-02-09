// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// IDE_MODE

enum class MyEnum1 {
    X, Y
}

enum class MyEnum2 {
    X, Y
}

enum class MyEnum3 {
    X, Y
}

fun foo(x: MyEnum1) {}
fun foo(x: MyEnum2) {}

fun bar(x: MyEnum3) {}

fun <X> id(x: X): X = TODO()

@JvmName("baz1")
fun <T1> baz(x: T1, y: (T1) -> Unit): T1 = TODO()
fun <T2> baz(x: T2, y: (String) -> Unit): T2 = TODO()

fun foobar(x: MyEnum3) {}

fun mightBeBetter(x: MyEnum1, y: String) {}
fun mightBeBetter(x: MyEnum1, y: Int) {}

fun main() {
    foo(MyEnum1.X)
    foo(id(MyEnum1.X))

    bar(<!DEBUG_INFO_CSR_MIGHT_BE_USED!>MyEnum3.X<!>)

    // Do not report DEBUG_INFO_CSR_MIGHT_BE_USED because we use information from the first argument
    // to disambiguate between the overloads, while for CSR, we don't add the `MyEnum1.X <: T` constraint
    // until the completion
    val x3: MyEnum1 = baz(MyEnum1.X, { x: String -> })
    val x4: MyEnum1 = baz(id(MyEnum1.X), { x: String -> })

    fun foobar(x: String) {}

    // Do not report DEBUG_INFO_CSR_MIGHT_BE_USED because otherwise we would stop on the local overload as no contradictory constraints
    // are present there.
    foobar(MyEnum3.X)

    // Generally we might've suppored this case because we discriminate the overload via the second argument,
    // but at this moment it feels like a very subtle thing.
    mightBeBetter(MyEnum1.X, "")
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, functionDeclaration, nullableType, typeParameter */
