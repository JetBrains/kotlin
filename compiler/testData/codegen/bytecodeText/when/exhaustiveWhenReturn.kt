// IGNORE_BACKEND: JVM_IR
enum class A { V }

fun box(): String {
    val a: A = A.V
    when (a) {
        A.V -> return "OK"
    }
}

// 1 TABLESWITCH
// 0 LOOKUPSWITCH
// 1 ATHROW
