inline fun watch(p: String, f: (String) -> Int) {
    f(p)
}

fun main(args: Array<String>) {
    val local = "mno"
    watch(local) { it.length }
}

// JVM_IR_TEMPLATES
// 2 LOCALVARIABLE p
// 1 LOCALVARIABLE p\$iv
// JVM_IR_TEMPLATES_WITH_INLINE_SCOPES
// 2 LOCALVARIABLE p
// 0 LOCALVARIABLE p\$iv
