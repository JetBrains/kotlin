fun <T> id(x: T): T = x
fun <T> String.extId(x: T): T = x

fun <T> foo(value: T?): T? = value?.let(::id) // ::id = KFunction1<T!!, T!!>
fun <T> bar(value: T?): T? = value?.let(""::extId)

fun box() = foo("O")!! + bar("K")!!
