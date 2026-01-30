// LANGUAGE: +ExportKDocDocumentationToKlib

/** Class documentation. */
class Foo
/** Primary constructor docummentation. */
constructor(val value: String) {
    /** Secondary constructor documentation. */
    constructor(value: Int) : this(value.toString())

    /** Member function documentation. */
    fun foo() {}

    /** Member property documentation. */
    val prop: String = ""

    /** Companion object documentation. */
    companion object {}
}

/** Type alias documentation. */
typealias MyString = String

/** Annotation class documentation. */
annotation class Anno

/** Enum class documentation. */
enum class Direction {
    /** Enum value documentation. */
    SOUTH
}

/** Top-level object documentation. */
object Obj

/** Top-level function documentation. */
fun bar(number: Int) {}

/** Top-level property documentation. */
val topLevelProp: Int
    get() = 0