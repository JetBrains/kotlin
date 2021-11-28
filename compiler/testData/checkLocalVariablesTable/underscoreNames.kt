data class A(val x: Double = 1.0, val y: String = "", val z: Char = '0')

fun foo(a: A, block: (A, String, Int) -> String): String = block(a, "", 1)

val arrayOfA: Array<A> = Array(1) { A() }

fun box() {

    foo(A()) {
        (x, _, y), _, w ->

        val (a, _, c) = A()
        val (_, `_`, d) = A()

        for ((_, q) in arrayOfA) {

        }

        ""
    }
}

// METHOD : UnderscoreNamesKt$box$1.invoke(LA;Ljava/lang/String;I)Ljava/lang/String;
// VARIABLE : NAME=x TYPE=D INDEX=4
// VARIABLE : NAME=y TYPE=C INDEX=6
// VARIABLE : NAME=q TYPE=Ljava/lang/String; INDEX=16
// VARIABLE : NAME=d TYPE=C INDEX=11
// VARIABLE : NAME=_ TYPE=Ljava/lang/String; INDEX=10
// VARIABLE : NAME=c TYPE=C INDEX=9
// VARIABLE : NAME=a TYPE=D INDEX=7
// VARIABLE : NAME=this TYPE=LUnderscoreNamesKt$box$1; INDEX=0

// JVM_TEMPLATES
// VARIABLE : NAME=$dstr$x$_u24__u24$y TYPE=LA; INDEX=1
// VARIABLE : NAME=$noName_1 TYPE=Ljava/lang/String; INDEX=2
// VARIABLE : NAME=w TYPE=I INDEX=3

// JVM_IR_TEMPLATES
// VARIABLE : NAME=w TYPE=I INDEX=3
