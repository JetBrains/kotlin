import kotlin.reflect.KClass

/** should load cls */
annotation class SimpleAnn(val value: String)

/** should load cls */
annotation class Ann(
    val x: Int,
    val y: String,
    val z: KClass<*>,
    val e: Array<KClass<*>>,
    val depr: DeprecationLevel
    vararg val t: SimpleAnn
)

interface Base {
    @Ann(1, "134", String::class, arrayOf(Int::class, Double::class), DeprecationLevel.WARNING, SimpleAnn("243"), SimpleAnn("4324"))
    fun foo(
        @Ann(2, "324", Ann::class, arrayOf(Byte::class, Base::class), DeprecationLevel.WARNING, SimpleAnn("687"), SimpleAnn("78")) x: String
    )
}

class Derived(b: Base) : Base by b {

}
