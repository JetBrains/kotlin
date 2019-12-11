import kotlin.reflect.KClass

annotation class Ann(
        val a: Array<String> = arrayOf(readOnly),
        val b: Array<String> = arrayOf(withGetter),
        val c: Array<String> = arrayOf(func()),
        val d: IntArray = intArrayOf(ONE, twoWithGetter),
        val e: IntArray = intArrayOf(ONE + twoWithGetter),
        val f: Array<String> = arrayOf(mutable),
        val g: Array<String> = arrayOf(mutableWithGetter),
        val h: Array<KClass<*>> = arrayOf(WithLateinit.kClass)
)

const val ONE = 1

val twoWithGetter
    get() = 2

val readOnly = ""

val withGetter
    get() = ""

fun func() = ""

var mutable = ""

var mutableWithGetter
    get() = ""
    set(x) = TODO()

object WithLateinit {
    lateinit var kClass: KClass<*>
}