data class Data(val x: String, val y: Int)

suspend fun test() {
    foo(Data("A", 1)) { (x_param, y_param) ->
        "$x_param / $y_param"
    }
}

suspend fun foo(data: Data, body: suspend (Data) -> Unit) {
    body(data)
}

// METHOD : DataClassKt$test$2.doResume(Ljava/lang/Object;Ljava/lang/Throwable;)Ljava/lang/Object;
// VARIABLE : NAME=this TYPE=LDataClassKt$test$2; INDEX=0
// VARIABLE : NAME=data TYPE=Ljava/lang/Object; INDEX=1
// VARIABLE : NAME=throwable TYPE=Ljava/lang/Throwable; INDEX=2
// VARIABLE : NAME=$x_param_y_param TYPE=LData; INDEX=3
// VARIABLE : NAME=x_param TYPE=Ljava/lang/String; INDEX=4
// VARIABLE : NAME=y_param TYPE=I INDEX=5
