fun main(args : Array<String>) {
    val x = 1
    fun local0() = println(x)
    fun local1() {
        fun local2() {
            local1()
        }
        local0()
    }
    println("OK")
}