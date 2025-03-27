package foo

const val CONSTANT = 5
annotation class Anno(val s: String)

<expr>@Anno("str" + CONSTANT) @Anno("o")</expr>
