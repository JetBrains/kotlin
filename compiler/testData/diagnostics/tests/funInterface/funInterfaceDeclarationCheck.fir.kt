// !LANGUAGE: +FunctionalInterfaceConversion

fun interface Good {
    fun invoke()
}

fun interface Foo1

fun interface Foo2 {

}

fun interface Foo3 {
    fun foo()
    fun bar()
}

interface BaseWithSAM {
    fun base()
}

fun interface Foo4 : BaseWithSAM {
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

fun interface Foo5 {
    val prop: Int
}

fun interface Foo6 {
    fun foo()
    val prop: Int
}

fun interface Foo7 : BaseWithSAM {
    val prop: Int
}

fun interface GoodWithPropAndBase : BaseWithSAM {
    val prop: Int get() = 42
}

fun interface Foo8 {
    fun <T> invoke(x: T)
}

fun interface GoodGeneric<T> {
    fun invoke(x: T)
}

interface BaseWithGeneric {
    fun <T> invoke(x: T)
}

fun interface Foo9 : BaseWithGeneric

fun interface GoodExtensionGeneric : GoodGeneric<String>

fun interface GoodSuspend {
    suspend fun invoke()
}

class WithNestedFun<K> {
    fun interface NestedSimple

    fun interface GoodFun {
        fun invoke()
    }

    fun interface NestedFun {
        fun inovke(element: K)
    }
}

fun <T> local() {
    <!LOCAL_INTERFACE_NOT_ALLOWED!>fun interface LocalFun<!> {
        fun invoke(element: T)
    }
}

fun interface WithDefaultValue {
    fun invoke(s: String = "")
}

interface BaseWithDefaultValue {
    fun invoke(s: String = "")
}

fun interface DeriveDefault : BaseWithDefaultValue

