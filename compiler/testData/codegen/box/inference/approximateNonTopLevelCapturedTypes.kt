interface BasePublisher<U>

interface Flow<out P>

fun <R> asFlow(x: BasePublisher<R>): Flow<R> = null as Flow<R>

class Document<S>

interface DerivedPublisher<K>: BasePublisher<Document<K>> {}

class Foo<T>(val x: DerivedPublisher<out T>) : Flow<Document<out T>> by asFlow(x)

fun box() = "OK"