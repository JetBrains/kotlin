// LANGUAGE: +ExportKDocDocumentationToKlib

/**
 * Class
 * documentation.
 *
 * @author Me myself.
 * @since Forever.
 *
 * @property value Not for swear words.
 */
class Foo
/**
 * Primary
 * constructor
 * docummentation.
 */
constructor(val value: String) {
    /**
     * Secondary
     * constructor
     * documentation.
     *
     * @param value Some integer. Maybe bigger is better, who knows.
     */
    constructor(value: Int) : this(value.toString())

    /**
     * Member
     * function
     * documentation.
     *
     * @return Nothing really.
     * @throws Throwable
     *
     * @see [prop] if you forgot how properties look.
     */
    fun foo() {}

    /**
     * Member
     * property
     * documentation.
     */
    val prop: String = ""

    /**
     * Companion
     * object
     * documentation.
     */
    companion object {}
}

/**
 * Type
 * alias
 * documentation.
 */
typealias MyString = String

/**
 * Annotation
 * class
 * documentation.
 */
annotation class Anno

/**
 * Enum
 * class
 * documentation.
 */
enum class Direction {
    /**
     * Enum
     * value
     * documentation.
     */
    SOUTH
}

/**
 * Top-level
 * object
 * documentation.
 */
object Obj

/**
 * Top-level
 * function
 * documentation.
 */
fun bar(number: Int) {}

/**
 * Top-level
 * property
 * documentation.
 */
val topLevelProp: Int
    get() = 0