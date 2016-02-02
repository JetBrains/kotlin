inline fun watch(p: String, f: (String) -> Int) {
    f(p)
}

fun main(args: Array<String>) {
    val local = "mno"
    watch(local) { it.length }
}

// 2 LOCALVARIABLE p
// 1 LOCALVARIABLE p\$iv