class A {
    public var prop = "O"
        private set

    fun test() {
        { prop }()
    }
}

// 0 INVOKESTATIC test\/A\.access\$getProp\$0
// 1 INVOKEVIRTUAL A\.getProp