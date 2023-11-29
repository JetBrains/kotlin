// !RENDER_DIAGNOSTICS_FULL_TEXT
// FIR_DUMP

fun bar() {
    <!NEW_INFERENCE_ERROR!>buildList {
        add("Boom")
        println(this.plus(1)[0])
    }<!>
}
