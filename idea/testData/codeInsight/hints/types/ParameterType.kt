// MODE: parameter
fun <T> T.wrap(lambda: (T) -> T) {}
fun foo() {
    12.wrap { elem<# : Int #> ->
        elem
    }
}
