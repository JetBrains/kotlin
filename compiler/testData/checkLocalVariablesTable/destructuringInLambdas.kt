// LAMBDAS: CLASS

data class A(val x: String, val y: Int)

fun foo(a: A, block: (A) -> String): String = block(a)

fun box() {
    foo(A("O", 123)) { (x, y) -> x + y }
}

// METHOD : DestructuringInLambdasKt$box$1.invoke(LA;)Ljava/lang/String;
// VARIABLE : NAME=this TYPE=LDestructuringInLambdasKt$box$1;
// VARIABLE : NAME=x TYPE=Ljava/lang/String;
// VARIABLE : NAME=y TYPE=I
