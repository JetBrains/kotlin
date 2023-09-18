fun test() {
    class Local {
        fun foo(): String {
            <expr>return "foo"</expr>
        }
    }

    Local().foo()
}