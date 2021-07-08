fun <T> simpleRun(f: (T) -> Unit): Unit = f(<!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!>)

fun <T, R> List<T>.simpleMap(f: (T) -> R): R {

<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

fun <T> simpleWith(t: T, f: T.() -> Unit): Unit = t.f()

interface KMutableProperty1<T, R> : KProperty1<T, R>, KMutableProperty<R>

interface KProperty1<T, out R> : KProperty<R>, (T) -> R

interface KProperty<out R>

interface KMutableProperty<R>

