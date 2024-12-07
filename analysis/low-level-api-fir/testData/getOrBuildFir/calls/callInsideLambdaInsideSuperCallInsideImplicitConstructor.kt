open class IJProject(val init: () -> Unit = {})

class KotlinIDE : IJProject(init = {
    fun foo() {
        <expr>anySymbols</expr>
    }
})
