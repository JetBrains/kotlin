package test

inline fun Double.test(a: Int, b: Long, c: () -> String): String {
    return "${this}_${a}_${b}_${c()}"
}