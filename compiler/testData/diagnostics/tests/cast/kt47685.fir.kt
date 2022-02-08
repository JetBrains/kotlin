// KT-47685
interface KtFunction {
    fun foo() {}
}

abstract class ASTDelegatePsiElement {
    fun bar() {}
}

class KtNamedFunction : ASTDelegatePsiElement(), KtFunction {
    fun baz() {}
}
class KtFunctionLiteral : ASTDelegatePsiElement(), KtFunction

fun test_1(namedFunction: KtNamedFunction, functionLiteral: KtFunctionLiteral, cond: Boolean) {
    val function = when (cond) {
        true -> namedFunction
        false -> functionLiteral
    }
    // approve that foo has type (ASTDelegatePsiElement & KtFunction)
    function.foo()
    function.bar()

    if (function is KtNamedFunction) {
        function.baz()
    }

    val myNamedFunction = function as KtNamedFunction
}
