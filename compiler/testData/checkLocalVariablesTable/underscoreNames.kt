// LAMBDAS: CLASS

data class A(val x: Double = 1.0, val y: String = "", val z: Char = '0')

fun foo(a: A, block: (A, String, Int) -> String): String = block(a, "", 1)

val arrayOfA: Array<A> = Array(1) { A() }

fun box() {

    foo(A()) {
        (x, _, y), _, w ->

        val (a, _, c) = A()
        val (_, `_`, d) = A()

        for ((_, q) in arrayOfA) {
            Unit
        }

        ""
    }
}

// METHOD : UnderscoreNamesKt$box$1.invoke(LA;Ljava/lang/String;I)Ljava/lang/String;
// VARIABLE : NAME=_ TYPE=Ljava/lang/String;
// VARIABLE : NAME=a TYPE=D
// VARIABLE : NAME=c TYPE=C
// VARIABLE : NAME=d TYPE=C
// VARIABLE : NAME=q TYPE=Ljava/lang/String;
// VARIABLE : NAME=this TYPE=LUnderscoreNamesKt$box$1;
// VARIABLE : NAME=w TYPE=I
// VARIABLE : NAME=x TYPE=D
// VARIABLE : NAME=y TYPE=C
