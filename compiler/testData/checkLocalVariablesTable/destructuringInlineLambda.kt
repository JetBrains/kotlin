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
// VARIABLE : NAME=i TYPE=I INDEX=2
// VARIABLE : NAME=$dstr$a1$a2$a3 TYPE=LStation; INDEX=1
// VARIABLE : NAME=a1 TYPE=Ljava/lang/String; INDEX=4
// VARIABLE : NAME=a2 TYPE=Ljava/lang/String; INDEX=5
// VARIABLE : NAME=a3 TYPE=I INDEX=6
// VARIABLE : NAME=$i$a$-foo-DestructuringInlineLambdaKt$box$1 TYPE=I INDEX=3
// VARIABLE : NAME=$i$f$foo TYPE=I INDEX=0
