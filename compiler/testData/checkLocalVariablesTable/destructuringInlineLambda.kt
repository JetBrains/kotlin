// IGNORE_BACKEND: JVM_IR
inline fun foo(x: (Int, Station) -> Unit) {
    x(1, Station(null, "", 1))
}

data class Station(
        val id: String?,
        val name: String,
        val distance: Int)

fun box(): String {
    foo { i, (a1, a2, a3) -> i + a3 }
    return "OK"
}

// METHOD : DestructuringInlineLambdaKt.box()Ljava/lang/String;
// VARIABLE : NAME=i TYPE=I INDEX=1
// VARIABLE : NAME=$a1_a2_a3 TYPE=LStation; INDEX=0
// VARIABLE : NAME=a1 TYPE=Ljava/lang/String; INDEX=2
// VARIABLE : NAME=a2 TYPE=Ljava/lang/String; INDEX=3
// VARIABLE : NAME=a3 TYPE=I INDEX=4
// VARIABLE : NAME=$i$a$1$foo TYPE=I INDEX=5
// VARIABLE : NAME=$i$f$foo TYPE=I INDEX=6
