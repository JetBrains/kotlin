annotation class MyAnno<C: Any>(val data: KClass<C>)

@MyAnno<Int>(Int::class)
fun fo<caret>o() {}
