// TYPE: 'n'
// OUT_OF_CODE_BLOCK: FALSE

fun println(s: String) {

}

class InSecondaryConstructor {
    init {
        println("Init block")
    }

    constructor(i: Int) {
        printl<caret>("Constructor")
    }
}