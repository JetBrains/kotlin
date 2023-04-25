// FIR_IDENTICAL
// !LANGUAGE: +FunctionalInterfaceConversion

fun interface Good {
    fun invoke()
}

<!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface Foo1

<!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface Foo2 {

}

<!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface Foo3 {
    fun foo()
    fun bar()
}

interface BaseWithSAM {
    fun base()
}

<!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface Foo4 : BaseWithSAM {
    fun oneMore()
}

fun interface Foo4WithDefault : BaseWithSAM {
    fun oneMore() {}
}

interface BaseWithDefault {
    fun def() {}
}

fun interface Foo4WithBaseDefault : BaseWithDefault {
    fun oneMore()
}

fun interface GoodWithBase : BaseWithSAM

<!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface Foo5 {
    <!FUN_INTERFACE_CANNOT_HAVE_ABSTRACT_PROPERTIES!>val<!> prop: Int
}

fun interface Foo6 {
    fun foo()
    <!FUN_INTERFACE_CANNOT_HAVE_ABSTRACT_PROPERTIES!>val<!> prop: Int
}

fun interface Foo7 : BaseWithSAM {
    <!FUN_INTERFACE_CANNOT_HAVE_ABSTRACT_PROPERTIES!>val<!> prop: Int
}

fun interface GoodWithPropAndBase : BaseWithSAM {
    val prop: Int get() = 42
}

fun interface Foo8 {
    fun <!FUN_INTERFACE_ABSTRACT_METHOD_WITH_TYPE_PARAMETERS!><T><!> invoke(x: T)
}

fun interface GoodGeneric<T> {
    fun invoke(x: T)
}

interface BaseWithGeneric {
    fun <T> invoke(x: T)
}

<!FUN_INTERFACE_ABSTRACT_METHOD_WITH_TYPE_PARAMETERS!>fun<!> interface Foo9 : BaseWithGeneric

fun interface GoodExtensionGeneric : GoodGeneric<String>

fun interface GoodSuspend {
    suspend fun invoke()
}

class WithNestedFun<K> {
    <!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface NestedSimple

    fun interface GoodFun {
        fun invoke()
    }

    fun interface NestedFun {
        fun inovke(element: <!UNRESOLVED_REFERENCE!>K<!>)
    }
}

fun <T> local() {
    <!LOCAL_INTERFACE_NOT_ALLOWED!>fun interface LocalFun<!> {
        fun invoke(element: T)
    }
}

fun interface WithDefaultValue {
    fun invoke(<!FUN_INTERFACE_ABSTRACT_METHOD_WITH_DEFAULT_VALUE!>s: String = ""<!>)
}

interface BaseWithDefaultValue {
    fun invoke(s: String = "")
}

<!FUN_INTERFACE_ABSTRACT_METHOD_WITH_DEFAULT_VALUE!>fun<!> interface DeriveDefault : BaseWithDefaultValue

