class A(val value: String)

fun A.test(): String {
    val o = object  {
        val z: String
        init {
            val x = value + "K"
            z = x
        }
    }
    return o.z
}

// METHOD : Kt11117Kt$test$o$1.<init>(LA;)V
// VARIABLE : NAME=$receiver TYPE=LA;
// VARIABLE : NAME=this TYPE=LKt11117Kt$test$o$1;
// VARIABLE : NAME=x TYPE=Ljava/lang/String;
