data class A(val x: String, val y: String)

fun foo(a: A, block: (A) -> String): String = block(a)

fun box() {
    foo(A("O", "K")) { (x, y) -> x + y }
}

// METHOD : DestructuringInLambdasKt$box$1.invoke(LA;)Ljava/lang/String;
// VARIABLE : NAME=this TYPE=LDestructuringInLambdasKt$box$1; INDEX=0
// VARIABLE : NAME=$x_y TYPE=LA; INDEX=1
// VARIABLE : NAME=x TYPE=LA; INDEX=2
// VARIABLE : NAME=y TYPE=LA; INDEX=3
