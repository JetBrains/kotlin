fun foo() {
    class Local {
    }
    val a<caret> = fun (): Local {
        return Local()
    }
}
