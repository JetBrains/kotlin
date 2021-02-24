// MODE: return
val x = run {
    when (true) {
        true -> 1<# ^run #>
        false -> 0<# ^run #>
    }
}