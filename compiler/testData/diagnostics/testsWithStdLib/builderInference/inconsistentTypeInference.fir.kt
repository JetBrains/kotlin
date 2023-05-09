// !RENDER_DIAGNOSTICS_FULL_TEXT
// FIR_DUMP

fun foo() {
    buildList {
        add("Boom")
        println(plus(1)[0])
    }
}

