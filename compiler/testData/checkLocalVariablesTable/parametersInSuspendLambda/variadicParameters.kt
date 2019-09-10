// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// !LANGUAGE: +NewInference +VariadicGenerics

suspend fun <vararg Ts, R> variadic(
    first: Any,
    vararg args: *Ts,
    transform: suspend (Any, *Ts) -> R
) {
    transform(first, args)
}

suspend fun test() {
    variadic( "boo", 1, 2, "foo") { boo, one, two, foo ->
        one + two
    }
}

// METHOD : VariadicParametersKt$test$2.invokeSuspend(Ljava/lang/Object;)Ljava/lang/Object;
// VARIABLE : NAME=boo TYPE=Ljava/lang/Object; INDEX=2
// VARIABLE : NAME=variadicLambdaArguments TYPE=Lkotlin/Tuple; INDEX=3
// VARIABLE : NAME=one TYPE=I INDEX=4
// VARIABLE : NAME=two TYPE=I INDEX=5
// VARIABLE : NAME=foo TYPE=Ljava/lang/String; INDEX=6
// VARIABLE : NAME=this TYPE=LVariadicParametersKt$test$2; INDEX=0
// VARIABLE : NAME=$result TYPE=Ljava/lang/Object; INDEX=1