// WITH_STDLIB
data class A(val x: String, val y: String)

suspend fun foo(block: suspend Long.(Int, String, Int, A) -> String): String = 1L.block(1, "O", 2, A("K", "!"))

suspend fun test() = foo { used, unused, _, (x, _) ->
    used.toString() + x
}

// METHOD : UnusedParametersKt$test$2.invokeSuspend(Ljava/lang/Object;)Ljava/lang/Object;
// VARIABLE : NAME=$result TYPE=Ljava/lang/Object;
// VARIABLE : NAME=<destruct> TYPE=LA;
// VARIABLE : NAME=this TYPE=LUnusedParametersKt$test$2;
// VARIABLE : NAME=unused TYPE=Ljava/lang/String;
// VARIABLE : NAME=used TYPE=I
// VARIABLE : NAME=x TYPE=Ljava/lang/String;
