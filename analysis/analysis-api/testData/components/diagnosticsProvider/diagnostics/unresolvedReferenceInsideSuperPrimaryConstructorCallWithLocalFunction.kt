open class IJProject(val init: () -> Unit = {})

@Suppress("UNUSED_VARIABLE")
class KotlinIDE() : IJProject(init = {
    fun foo() {
        val f = anySymbols
    }
})
