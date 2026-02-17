class C {
    fun inTrailing(x: String, block: (String) -> String = { x.uppercase() }): String = "$x/${block("")}"
    fun inArgument(x: String, block: (String) -> String): String = "$x/${block("")}"
}