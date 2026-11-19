import kotlin.reflect.KClass

annotation class Anno(val value: KClass<*>)

fun test() {
    @Anno(<expr>Local::class</expr>)
    class Local
}
