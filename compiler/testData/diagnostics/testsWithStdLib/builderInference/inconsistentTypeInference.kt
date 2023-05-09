// !RENDER_DIAGNOSTICS_FULL_TEXT
// FIR_DUMP

fun foo() {
    buildList {
        add("Boom")
        println(<!RECEIVER_TYPE_MISMATCH!>plus<!>(1)[0])
    }
}

