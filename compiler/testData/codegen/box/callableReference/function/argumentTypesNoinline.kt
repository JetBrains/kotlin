
fun <T> id(x: T): T = x
fun <T> String.extId(x: T): T = x

fun <T, R> T.myLet(block: (T) -> R): R = block(this)

fun <T> foo(value: T?): T? = value?.myLet(::id) // ::id = KFunction1<T!!, T!!>
fun <T> bar(value: T?): T? = value?.myLet(""::extId)

fun box() = foo("O")!! + bar("K")!!
