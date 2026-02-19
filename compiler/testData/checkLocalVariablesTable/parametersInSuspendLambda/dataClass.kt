// WITH_STDLIB
data class Data(val x: String, val y: Int)

suspend fun test() {
    foo(Data("A", 1)) { (x_param, y_param) ->
        "$x_param / $y_param"
    }
}

suspend fun foo(data: Data, body: suspend (Data) -> Unit) {
    body(data)
}

// METHOD : DataClassKt$test$2.invokeSuspend(Ljava/lang/Object;)Ljava/lang/Object;
// VARIABLE : NAME=$result TYPE=Ljava/lang/Object;
// VARIABLE : NAME=<destruct> TYPE=LData;
// VARIABLE : NAME=this TYPE=LDataClassKt$test$2;
// VARIABLE : NAME=x_param TYPE=Ljava/lang/String;
// VARIABLE : NAME=y_param TYPE=I
