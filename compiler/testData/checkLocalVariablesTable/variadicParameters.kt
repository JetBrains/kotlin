// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JVM
// WITH_RUNTIME
// !LANGUAGE: +NewInference

fun <vararg Ts> variadic(
    first: Any,
    vararg args: *Ts,
    transform: (Any, *Ts) -> Unit
) {
    transform(first, args)
}

fun test() {
    variadic( "boo", 1, 2, "foo") { boo, one, two, foo ->
        one + two
    }
}

// METHOD : VariadicParametersKt$test$1.invoke(Ljava/lang/Object;Lkotlin/Tuple;)V
// VARIABLE : NAME=this TYPE=LVariadicParametersKt$test$1; INDEX=0
// VARIABLE : NAME=boo TYPE=Ljava/lang/Object; INDEX=1
// VARIABLE : NAME=variadicLambdaArguments TYPE=Lkotlin/Tuple; INDEX=2
// VARIABLE : NAME=one TYPE=I INDEX=3
// VARIABLE : NAME=two TYPE=I INDEX=4
// VARIABLE : NAME=foo TYPE=Ljava/lang/String; INDEX=5