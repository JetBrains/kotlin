open class IJProject(val init: () -> Unit = {})

class KotlinIDE : IJProject {
    constructor() : super(init = {
        anySymbols
    })
}