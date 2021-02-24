class Host {
    private fun bar() {
        class NamedLocal {
            fun run() {
                foo()
            }
        }
    }

    fun foo() {}
}

// 1 final synthetic LHost; this\$0
