package one

annotation class Anno<T : Number>(val value: KClass<T>)

@Anno<Int>(Int::class)
fun resolve<caret>Me() {

}