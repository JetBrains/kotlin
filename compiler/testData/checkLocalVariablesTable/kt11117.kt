// IGNORE_BACKEND: JVM_IR
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
// VARIABLE : NAME=x TYPE=Ljava/lang/String; INDEX=2
// VARIABLE : NAME=this TYPE=LKt11117Kt$test$o$1; INDEX=0
// VARIABLE : NAME=$receiver TYPE=LA; INDEX=1

