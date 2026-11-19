// WITH_STDLIB
context(s: String) fun useContext(): String = s

suspend fun foo(block: suspend context(String, Int, Int) Long.(Int) -> String): String = block("O", 1, 2, 3L, 4)

suspend fun test() = foo { x ->
    useContext() + x
}

// METHOD : UnusedContextParametersKt$test$2.invokeSuspend(Ljava/lang/Object;)Ljava/lang/Object;
// VARIABLE : NAME=$context-Int$1 TYPE=I
// VARIABLE : NAME=$context-Int$2 TYPE=I
// VARIABLE : NAME=$context-String TYPE=Ljava/lang/String;
// VARIABLE : NAME=$result TYPE=Ljava/lang/Object;
// VARIABLE : NAME=this TYPE=LUnusedContextParametersKt$test$2;
// VARIABLE : NAME=x TYPE=I
