// MODE: return
val x = run {
    if (true) {
        1<# ^run #>
    } else {
        0<# ^run #>
    }
}