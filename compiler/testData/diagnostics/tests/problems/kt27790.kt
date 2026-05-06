// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-27790
// WITH_STDLIB

// KT-27790: "Type checking has run into a recursive problem" on private property (with same name) in a class with declaration-site variance from a generic extension method

inline fun <reified U> U.myName(): String = U::class.simpleName ?: "simpleName"

class User<out T> {
    // This fails in K1 with "Type checking has run into a recursive problem"
    private val myName = myName()
}

class Person<T> {
    // This works in K1
    private val myName = myName()
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, elvisExpression, funWithExtensionReceiver, functionDeclaration,
inline, nullableType, out, propertyDeclaration, reified, stringLiteral, typeParameter */
