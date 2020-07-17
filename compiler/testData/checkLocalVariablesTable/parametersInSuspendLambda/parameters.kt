// WITH_RUNTIME
data class Data(val x: String, val y: Int, val z: Int = 0)

suspend fun test() {
    foo(Data("A", 1)) { str, (x, _, z), i ->
        println(str + x + z + i + this)
    }
}

suspend fun foo(data: Data, body: suspend Long.(String, Data, Int) -> Unit) {
    1L.body("OK", data, 1)
}

// Parameters (including anonymous destructuring parameters) are moved to fields in the Continuation class for the suspend lambda class.
// However, in non-IR, the fields are first stored in local variables, and they are not read directly (even for destructuring components).
// In IR, the fields are directly read from.

// METHOD : ParametersKt$test$2.invokeSuspend(Ljava/lang/Object;)Ljava/lang/Object;

// JVM_TEMPLATES
// VARIABLE : NAME=$this$foo TYPE=J INDEX=2
// VARIABLE : NAME=str TYPE=Ljava/lang/String; INDEX=4
// VARIABLE : NAME=$dstr$x$_u24__u24$z TYPE=LData; INDEX=5
// VARIABLE : NAME=i TYPE=I INDEX=6
// VARIABLE : NAME=x TYPE=Ljava/lang/String; INDEX=7
// VARIABLE : NAME=z TYPE=I INDEX=8
// VARIABLE : NAME=this TYPE=LParametersKt$test$2; INDEX=0
// VARIABLE : NAME=$result TYPE=Ljava/lang/Object; INDEX=1

// JVM_IR_TEMPLATES
// VARIABLE : NAME=x TYPE=Ljava/lang/String; INDEX=2
// VARIABLE : NAME=z TYPE=I INDEX=3
// VARIABLE : NAME=this TYPE=LParametersKt$test$2; INDEX=0
// VARIABLE : NAME=$result TYPE=Ljava/lang/Object; INDEX=1
