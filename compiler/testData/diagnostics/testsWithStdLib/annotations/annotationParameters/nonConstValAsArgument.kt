// FIR_IDENTICAL
import kotlin.reflect.KClass

annotation class Ann(
        val a: Array<String> = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT!>arrayOf(readOnly)<!>,
        val b: Array<String> = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT!>arrayOf(withGetter)<!>,
        val c: Array<String> = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT!>arrayOf(func())<!>,
        val d: IntArray = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT!>intArrayOf(ONE, twoWithGetter)<!>,
        val e: IntArray = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT!>intArrayOf(ONE + twoWithGetter)<!>,
        val f: Array<String> = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT!>arrayOf(mutable)<!>,
        val g: Array<String> = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT!>arrayOf(mutableWithGetter)<!>,
        val h: Array<KClass<*>> = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT!>arrayOf(WithLateinit.kClass)<!>
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
