fun main(args : Array<String>) {
    val x = 1
    fun local0() = println(x)
    fun local1() {
        fun local2() {
            local1()
        }
        local0()
    }

    fun l1() {
        var x = 1
        fun l2() {
            fun l3() {
                l1()
                x = 5
            }
            l3()
        }
    }

    println("OK")
}