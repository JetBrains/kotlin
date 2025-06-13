// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
interface Test {
    fun foo()
    val bar: Int
}

fun Test?.foo() {}
val Test?.bar: Int get() = 42

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, getter, integerLiteral, interfaceDeclaration,
nullableType, propertyDeclaration, propertyWithExtensionReceiver */
