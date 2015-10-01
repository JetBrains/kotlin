package test

inline fun test(a: Int, b: Long, crossinline c: () -> String): String {
    return { "${a}_${b}_${c()}"} ()
}