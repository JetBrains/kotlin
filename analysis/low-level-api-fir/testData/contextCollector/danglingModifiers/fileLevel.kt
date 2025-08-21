package foo

const val CONSTANT = 5
annotation class Anno(val s: String)

@Anno("str" + <expr>CONSTANT</expr>)
