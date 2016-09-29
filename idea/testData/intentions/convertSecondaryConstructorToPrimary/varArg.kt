// WITH_RUNTIME

class WithVarArg {

    val x: List<String>

    constructor(<caret>vararg zz: String) {
        x = listOf(*zz)
    }
}