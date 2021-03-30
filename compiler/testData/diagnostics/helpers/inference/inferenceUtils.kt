fun <K> id(x: K): K = x
fun <K> materialize(): K = null!!
fun <K> select(vararg values: K): K = values[0]
