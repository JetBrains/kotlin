// MODE: return
val x = run hello@{
    if (true) {
    }

    run { // Two hints here
        when (true) {
            true -> 1<# ^run #>
            false -> 0<# ^run #>
        }
    }<# ^hello #>
}