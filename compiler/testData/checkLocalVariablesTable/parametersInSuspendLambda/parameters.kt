// WITH_STDLIB
data class Data(val x: String, val y: Int, val z: Int = 0)

suspend fun test() {
    foo(Data("A", 1)) { str, (x, _, z), i ->
        println(str + x + z + i + this)
    }
}

suspend fun foo(data: Data, body: suspend Long.(String, Data, Int) -> Unit) {
    1L.body("OK", data, 1)
}

// METHOD : ParametersKt$test$2.invokeSuspend(Ljava/lang/Object;)Ljava/lang/Object;
// VARIABLE : NAME=<destruct> TYPE=LData; INDEX=*
// VARIABLE : NAME=$result TYPE=Ljava/lang/Object; INDEX=*
// VARIABLE : NAME=$this$foo TYPE=J INDEX=*
// VARIABLE : NAME=i TYPE=I INDEX=*
// VARIABLE : NAME=str TYPE=Ljava/lang/String; INDEX=*
// VARIABLE : NAME=this TYPE=LParametersKt$test$2; INDEX=*
// VARIABLE : NAME=x TYPE=Ljava/lang/String; INDEX=*
// VARIABLE : NAME=z TYPE=I INDEX=*
