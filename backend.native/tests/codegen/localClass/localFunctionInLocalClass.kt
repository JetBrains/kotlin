fun main(args : Array<String>) {
    var x = 0
    class A {
        fun bar() {
            fun local() {
                class B {
                    fun baz() {
                        fun local2() {
                            x++
                        }
                        local2()
                    }
                }
                B().baz()
            }
            local()
        }
    }
    A().bar()
    println("OK")
}