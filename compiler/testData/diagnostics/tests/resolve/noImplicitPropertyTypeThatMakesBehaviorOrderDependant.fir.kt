// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// IGNORE_REVERSED_RESOLVE
// ISSUE: KT-76240

fun Int.lazyDecl() = toString()

class C1 {
    val lazyDecl by lazy { 42.lazyDecl() }
}

class C2 {
    // Unforunately, the warning `IMPLICIT_PROPERTY_TYPE_MAKES_BEHAVIOR_ORDER_DEPENDANT` is reported here but it should not.
    // Currently it's unclear how to get rid of it with the existing compiler architecture.
    // Hopefully, such a case is not uncommon, because it requires the call to be:
    //  * Having an extension receiver
    //  * Declaraed in an implicit body (implicit body resolve stage)
    //  * Being resolved to a property that:
    //    * Has implicit type
    //    * Has name that matches the call name
    //    * Declared below the use-site (it's implicit type is not yet resolved)
    // AA test in reversed mode doesn't report the diagnostic and that's why it's ignored
    fun test() = "str".<!IMPLICIT_PROPERTY_TYPE_MAKES_BEHAVIOR_ORDER_DEPENDANT!>extDecl<!>()

    val String.extDecl
        get() = "Extension property in C2"
}

fun String.extDecl() = "Extension top-level function"

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, integerLiteral, lambdaLiteral,
propertyDeclaration, propertyDelegate */
