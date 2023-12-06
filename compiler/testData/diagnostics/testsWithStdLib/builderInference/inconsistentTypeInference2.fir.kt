// !RENDER_DIAGNOSTICS_FULL_TEXT
// FIR_DUMP

fun bar() {
    buildList {
        add("Boom")
        println(this.plus(1)[0])
    }
}
