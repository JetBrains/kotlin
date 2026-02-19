// ISSUE: KT-76675

inline fun <R1> myRun1(x: () -> R1): R1 = TODO()
inline fun <R2> myRun2(x: () -> R2): R2 = TODO()

fun main() {
    myRun1 {
        ""
        <expr>myRun2 {
            return@myRun1 1
        }</expr>
    }
}