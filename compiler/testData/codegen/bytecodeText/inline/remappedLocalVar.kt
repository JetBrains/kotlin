// IGNORE_BACKEND: JVM_IR
inline fun watch(p: String, f: (String) -> Int) {
    f(p)
}

fun main(args: Array<String>) {
    val local = "mno"
    watch(local) { it.length }
}

/*fix rollbacked cause of robovm problem*/
// 1 LOCALVARIABLE p
// 0 LOCALVARIABLE p\$iv