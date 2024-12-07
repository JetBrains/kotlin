// MODULE: classifiers_library

package classifiers.test

class RegularClass(val property: String) {
    fun function(): String = ""
}

annotation class AnnotationClass(val property: String) {
    //fun function(): String = ""
}

value class ValueClass(val property: String) {
    fun function(): String = ""
}

data class DataClass(val property: String) {
    fun function(): String = ""
}

object Object {
    val property: String = ""
    fun function(): String = ""
}

interface Interface {
    val property: String
    fun function(): String
}

fun interface FunctionInterface {
    //val property: String
    fun function(): String
}

enum class EnumClassWithoutEntryClasses {
    FOO_NO_CLASS, BAR_NO_CLASS, BAZ_NO_CLASS
}

enum class EnumClassWithEntryClasses {
    FOO_HAS_CLASS {
        override val overriddenProperty get() = ""
        override fun overriddenFunction() = ""
        val fooOwnProperty get() = ""
        fun fooOwnFunction() = ""
        inner class FooInner
    },
    BAR_NO_CLASS,
    BAZ_HAS_CLASS {
        override val overriddenProperty get() = ""
        override fun overriddenFunction() = ""
        val bazOwnProperty get() = ""
        fun bazOwnFunction() = ""
        inner class BazInner
    };

    open val overriddenProperty: String get() = ""
    open fun overriddenFunction(): String = ""
}

class CompanionHolder1 {
    companion object // default name
}
class CompanionHolder2 {
    companion object Companion // custom name
}
class CompanionHolder3 {
    companion object DEFAULT // custom name
}
class CompanionHolder4 {
    object Object // a regular nested object
}

class TopLevelClass {
    class Nested {
        class Nested
        inner class Inner
    }
    inner class Inner {
        inner class Inner
    }
}
