// WITH_RUNTIME
data class Data(val x: String, val y: Int)

suspend fun test() {
    foo(Data("A", 1)) { (x_param, y_param) ->
        "$x_param / $y_param"
    }
}

suspend fun foo(data: Data, body: suspend (Data) -> Unit) {
    body(data)
}

// Parameters (including anonymous destructuring parameters) are moved to fields in the Continuation class for the suspend lambda class.
// However, in non-IR, the fields are first stored in local variables, and they are not read directly (even for destructuring components).
// In IR, the fields are directly read from.

// METHOD : DataClassKt$test$2.invokeSuspend(Ljava/lang/Object;)Ljava/lang/Object;

// JVM_TEMPLATES
// VARIABLE : NAME=$dstr$x_param$y_param TYPE=LData; INDEX=2
// VARIABLE : NAME=x_param TYPE=Ljava/lang/String; INDEX=3
// VARIABLE : NAME=y_param TYPE=I INDEX=4
// VARIABLE : NAME=this TYPE=LDataClassKt$test$2; INDEX=0
// VARIABLE : NAME=$result TYPE=Ljava/lang/Object; INDEX=1

// JVM_IR_TEMPLATES
// VARIABLE : NAME=x_param TYPE=Ljava/lang/String; INDEX=2
// VARIABLE : NAME=y_param TYPE=I INDEX=3
// VARIABLE : NAME=this TYPE=LDataClassKt$test$2; INDEX=0
// VARIABLE : NAME=$result TYPE=Ljava/lang/Object; INDEX=1
