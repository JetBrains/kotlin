package a

class InheritingClasses {
    abstract class A(override val c: Int = 1) : C {
        open fun of() = 3
        abstract fun af(): Int
        open val op = 4
        abstract val ap: Int
    }

    open class B : A(2) {
        override fun of() = 4
        override fun af() = 5
        override val op = 5
        override val ap = 5
    }

    trait C {
        val c: Int
    }

    trait D<T> : C {
        override val c: Int
    }

    trait E
    class G : B(), C, D<Int>, E


    class InheritAny {
        trait SomeTrait
        trait SomeTrait2

        class ImplicitAny

        class ExplicitAny : Any()

        class OnlyTrait : SomeTrait
        class OnlyTraits : SomeTrait, SomeTrait2

        class TraitWithExplicitAny : Any(), SomeTrait
        class TraitsWithExplicitAny : SomeTrait2, Any(), SomeTrait
    }

    abstract class InheritFunctionType : ((Int, String) -> Int)
    abstract class InheritExtensionFunctionType : (E.(Int, String) -> Int)
}