// !DIAGNOSTICS: -UNUSED_VARIABLE

val label_fun = label@ fun () {
    return@label
}

val parenthesized_label_fun = (label@ fun () {
    return@label
})
