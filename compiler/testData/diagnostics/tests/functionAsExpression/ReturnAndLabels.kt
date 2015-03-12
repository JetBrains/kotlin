// !DIAGNOSTICS: -UNUSED_VARIABLE

val label_fun = @label fun () {
    return@label
}

val parenthesized_label_fun = (@label fun () {
    return@label
})

val fun_with_name = fun name() {
    return@name
}