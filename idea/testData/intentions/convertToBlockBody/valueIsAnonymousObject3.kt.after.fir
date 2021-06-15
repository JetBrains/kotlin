interface I1
interface I2

fun f() {
    fun g(): I1 {
        return object : I1, I2 { }
    }
}
