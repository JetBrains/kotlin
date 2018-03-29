val nonConstArray = longArrayOf(0)

annotation class Anno(vararg val value: Long)

@Anno(value = nonConstArray)
fun foo1() {}