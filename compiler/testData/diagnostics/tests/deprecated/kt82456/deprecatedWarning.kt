// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +CompanionBlocksAndExtensions

@Deprecated("Object", level = DeprecationLevel.WARNING)
object Object {
    operator fun invoke() { }
}

@Deprecated("CompanionBlock", level = DeprecationLevel.WARNING)
class CompanionBlock private constructor() {
    companion {
       operator fun invoke() { }
    }
}

fun test() {
    <!DEPRECATION!>Object<!>()
    <!DEPRECATION!>CompanionBlock<!>()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, objectDeclaration, operator, primaryConstructor,
stringLiteral */
