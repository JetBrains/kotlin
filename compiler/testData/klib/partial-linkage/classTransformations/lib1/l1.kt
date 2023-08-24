class ClassToEnum {
    class Foo
    object Bar
    inner class Baz
}

object ObjectToEnum {
    class Foo
    object Bar
}

enum class EnumToClass {
    Foo,
    Bar,
    Baz
}

enum class EnumToObject {
    Foo,
    Bar
}

class ClassToObject
object ObjectToClass

class ClassToInterface

class NestedObjectToCompanion1 {
    object Companion {
        fun name() = "NestedObjectToCompanion1.Companion"
        override fun toString() = name()
    }
}

class NestedObjectToCompanion2 {
    object Foo {
        fun name() = "NestedObjectToCompanion2.Foo"
        override fun toString() = name()
    }
}

class CompanionToNestedObject1 {
    companion object {
        fun name() = "CompanionToNestedObject1.Companion"
        override fun toString() = name()
    }
}

class CompanionToNestedObject2 {
    companion object Foo {
        fun name() = "CompanionToNestedObject2.Foo"
        override fun toString() = name()
    }
}

class CompanionAndNestedObjectsSwap {
    companion object Foo {
        fun name() = "Foo"
    }

    object Bar {
        fun name() = "Bar"
    }
}

class NestedClassContainer {
    fun name() = "NestedClassContainer"

    class NestedToInner {
        fun name() = "NestedClassContainer.NestedToInner"
        override fun toString() = name()

        object Object {
            fun name() = "NestedClassContainer.NestedToInner.Object"
            override fun toString() = name()
        }

        companion object Companion {
            fun name() = "NestedClassContainer.NestedToInner.Companion"
            override fun toString() = name()
        }

        class Nested {
            fun name() = "NestedClassContainer.NestedToInner.Nested"
            override fun toString() = name()
        }

        inner class Inner {
            fun name() = this@NestedToInner.name() + ".Inner"
            override fun toString() = name()
        }
    }
}

class InnerClassContainer {
    fun name() = "InnerClassContainer"

    inner class InnerToNested {
        fun name() = this@InnerClassContainer.name() + ".InnerToNested"
        override fun toString() = name()

        inner class /*object*/ Object {
            fun name() = this@InnerToNested.name() + ".Object"
            override fun toString() = name()
        }

        inner class /*companion object*/ Companion {
            fun name() = this@InnerToNested.name() + ".Companion"
            override fun toString() = name()
        }

        inner class /*class*/ Nested {
            fun name() = this@InnerToNested.name() + ".Nested"
            override fun toString() = name()
        }

        inner class Inner {
            fun name() = this@InnerToNested.name() + ".Inner"
            override fun toString() = name()
        }
    }
}

annotation class AnnotationClassWithChangedParameterType(val x: Int)
annotation class AnnotationClassThatBecomesRegularClass(val x: Int)
annotation class AnnotationClassThatDisappears(val x: Int)
annotation class AnnotationClassWithRenamedParameters(val i: Int, val s: String)
annotation class AnnotationClassWithReorderedParameters(val i: Int, val s: String)
annotation class AnnotationClassWithNewParameter(val i: Int)

value class ValueToClass(val x: Int)
class ClassToValue(val x: Int)

data class DataToClass(val x: Int, val y: Int)

class ClassToAbstractClass {
    var name: String = "Alice"
    fun getGreeting() = "Hello, $name!"
}

class RemovedClass
enum class EnumClassWithDisappearingEntry { UNCHANGED, REMOVED }

object PublicTopLevelLib1 {
    annotation class AnnotationClassThatBecomesPrivate
    class ClassThatBecomesPrivate
    enum class EnumClassThatBecomesPrivate { ENTRY }
}

interface XAnswer { fun answer(): Int }
interface XAnswerDefault { fun answer(): Int /*= 42*/ }
interface XFunction1 { /*fun function1(): Int*/ }
interface XFunction1Default { /*fun function1(): Int = 42*/ }
interface XFunction2 { /*fun function2(): Int*/ }
interface XFunction2Default { /*fun function2(): Int = -42*/ }
interface XProperty1 { /*val property1: Int*/ }
interface XProperty1Default { /*val property1: Int get() = 42*/ }
interface XProperty2 { /*val property2: Int*/ }
interface XProperty2Default { /*val property2: Int get() = 42*/ }

fun interface FunctionalInterfaceToInterface : XAnswer
