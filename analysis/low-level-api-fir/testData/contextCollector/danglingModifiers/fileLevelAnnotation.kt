package foo

const val CONSTANT = 5
annotation class Anno(val s: String)

@Anno("str" + CONSTANT) <expr>@Anno("o")</expr>
