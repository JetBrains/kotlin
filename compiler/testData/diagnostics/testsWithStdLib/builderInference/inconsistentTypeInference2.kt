// !RENDER_DIAGNOSTICS_FULL_TEXT
// FIR_DUMP

fun bar() {
    buildList {
        add("Boom")
        println(<!TYPE_MISMATCH!>this<!>.plus(1)[0])
    }
}
