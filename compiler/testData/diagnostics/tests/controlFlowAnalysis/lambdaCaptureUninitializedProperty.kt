// DIAGNOSTICS: -UNUSED_PARAMETER
// ISSUE: KT-70133

fun invokeLater(x: () -> Int, y: String) {
    x()
}

fun immutableInitAfterCapture() {
    val x: String
    invokeLater(
        x = { <!UNINITIALIZED_VARIABLE!>x<!>.length },
        y = if (true) { x = ""; "" } else { x = ""; "" },
    )
}

fun immutableInitBeforeCapture() {
    val x: String
    invokeLater(
        y = if (true) { x = ""; "" } else { x = ""; "" },
        x = { x.length },
    )
}

fun mutableInitAfterCapture() {
    var x: String
    invokeLater(
        x = { <!UNINITIALIZED_VARIABLE!>x<!>.length },
        y = if (true) { x = ""; "" } else { x = ""; "" },
    )
}

fun mutableInitBeforeCapture() {
    var x: String
    invokeLater(
        y = if (true) { x = ""; "" } else { x = ""; "" },
        x = { x.length },
    )
}
