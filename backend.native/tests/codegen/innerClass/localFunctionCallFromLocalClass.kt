fun main(args : Array<String>) {
    var x = 1
    fun local1() {
        x++
    }

    class A {
        fun bar() {
            local1()
        }
    }
    A().bar()
    println("OK")
}