// WITH_STDLIB

context(s: String) fun foo(x: Int): String = s + x

fun bar(f: context(String) (Int) -> Unit) {
    f("", 10)
}

fun box() {
    bar { x ->
        foo(x)
    }
}

// METHOD : ContextFunctionTypeLambdaKt.box$lambda$0(Ljava/lang/String;I)Lkotlin/Unit;
// VARIABLE : NAME=$context-String TYPE=Ljava/lang/String;
// VARIABLE : NAME=x TYPE=I
