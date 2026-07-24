class Foo {
    private fun foo() {
        class Local {
            fun call() {}
        }

        Lo<caret>cal().call()
    }
}