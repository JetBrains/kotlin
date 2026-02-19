// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
class Scope {
    fun test1(): String = "member "
    fun test2(): String = "member "
}

fun test1(): String = "top-level "

context(scope: Scope)
fun test2(): String = "context top level "

context(scope: Scope)
fun useWithContextInDeclaration(): String {
    return test1() +    //resolves to top-level
            test2()     //resolves to context top level
}

fun useWithContextPassedWithLambda(scope: Scope): String {
    return with(scope) {
        test1() +               //resolves to member
                test2()        //resolves to member
    }
}

fun box(): String {
    var result = "OK"
    with(Scope()) {
        if (useWithContextInDeclaration() != "top-level context top level ") result = "not OK"
        if (useWithContextPassedWithLambda(this) != "member member ") result = "not OK"
    }
    return result
}