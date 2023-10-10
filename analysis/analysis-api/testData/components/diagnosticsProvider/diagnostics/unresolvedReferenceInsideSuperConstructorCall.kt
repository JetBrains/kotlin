open class IJProject(val init: () -> Unit = {})

class KotlinIDE : IJProject(init = {
    anySymbols
})