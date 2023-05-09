// LAMBDAS: CLASS

class A {
    public var prop = "O"
        private set

    fun test() {
        val f = { prop }
        f()
    }
}

// 0 INVOKESTATIC test\/A\.access\$getProp\$0
// 1 INVOKEVIRTUAL A\.getProp
