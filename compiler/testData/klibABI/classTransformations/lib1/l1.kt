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
interface InterfaceToClass

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

value class ValueToClass(val x: Int)
class ClassToValue(val x: Int)

data class DataToClass(val x: Int, val y: Int)

fun interface FunctionalInterfaceToInterface {
    fun work()
}
