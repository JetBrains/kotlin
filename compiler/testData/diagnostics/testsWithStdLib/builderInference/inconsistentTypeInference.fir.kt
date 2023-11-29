// !RENDER_DIAGNOSTICS_FULL_TEXT
// FIR_DUMP

fun foo() {
    <!NEW_INFERENCE_ERROR!>buildList {
        add("Boom")
        println(plus(1)[0])
    }<!>
}

