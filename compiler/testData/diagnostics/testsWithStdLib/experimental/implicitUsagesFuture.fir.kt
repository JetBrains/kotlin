// !USE_EXPERIMENTAL: kotlin.RequiresOptIn
// LANGUAGE: -OptInContagiousSignatures

@RequiresOptIn
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.TYPEALIAS)
annotation class Marker

@Marker
interface Some

abstract class User {
    abstract fun createSome(): <!EXPERIMENTAL_API_USAGE_ERROR!>Some<!>
    fun <!EXPERIMENTAL_API_USAGE_ERROR!>Some<!>?.onSome() {}
    fun withSome(some: <!EXPERIMENTAL_API_USAGE_ERROR!>Some<!>? = null) {}

    fun use() {
        val something = <!EXPERIMENTAL_API_USAGE_ERROR!>createSome<!>()
        val somethingOther: <!EXPERIMENTAL_API_USAGE_ERROR!>Some<!> = <!EXPERIMENTAL_API_USAGE_ERROR!>createSome<!>()
        null.<!EXPERIMENTAL_API_USAGE_ERROR!>onSome<!>()
        <!EXPERIMENTAL_API_USAGE_ERROR!>withSome<!>()
    }
}

data class DataClass(@property:Marker val x: Int)

fun useDataClass(d: DataClass) {
    // Should have error in both
    d.<!EXPERIMENTAL_API_USAGE_ERROR!>x<!>
    val (x) = d
}

typealias My = <!EXPERIMENTAL_API_USAGE_ERROR!>Some<!>

fun my(my: <!EXPERIMENTAL_API_USAGE_ERROR!>My<!>) {}

fun your(my: <!EXPERIMENTAL_API_USAGE_ERROR!>Some<!>) {}

@Marker
interface ExperimentalType {
    fun foo() {}
    fun bar() {}
}

@OptIn(Marker::class)
interface NotExperimentalExtension : ExperimentalType {
    override fun foo() {}
}

fun use(arg: NotExperimentalExtension) {
    arg.foo()
    arg.<!EXPERIMENTAL_API_USAGE_ERROR!>bar<!>()
}

@Marker
interface I

@OptIn(Marker::class)
class A : I

@OptIn(Marker::class)
class B : I

@OptIn(Marker::class)
typealias MyList = ArrayList<I>

@Marker
typealias YourList = ArrayList<String>

fun main() {
    val x = <!EXPERIMENTAL_API_USAGE_ERROR!>listOf<!>(A(), B())
    val y = MyList()
    val z = YourList()
    YourList().add("")
}

@Marker
class C {
    operator fun getValue(x: Any?, y: Any?): String = ""
}

object O {
    @OptIn(Marker::class)
    operator fun provideDelegate(x: Any?, y: Any?): C = C()
}

val x: String by <!EXPERIMENTAL_API_USAGE_ERROR!>O<!>

@Marker
class OperatorContainer : Comparable<OperatorContainer> {
    @OptIn(Marker::class)
    override fun compareTo(other: OperatorContainer): Int {
        return 0
    }
}

@OptIn(Marker::class)
class AnotherContainer : Iterable<C> {
    @OptIn(Marker::class)
    override fun iterator(): Iterator<C> {
        return object : Iterator<C> {
            override fun hasNext(): Boolean {
                return false
            }

            override fun next(): C {
                throw java.util.NoSuchElementException()
            }
        }
    }
}

@OptIn(Marker::class)
operator fun String.minus(s: String) = OperatorContainer()

@OptIn(Marker::class)
operator fun String.invoke() = OperatorContainer()

fun operatorContainerUsage(s: String, a: AnotherContainer) {
    val res1 = s <!EXPERIMENTAL_API_USAGE_ERROR!>-<!> s
    val res2 = <!EXPERIMENTAL_API_USAGE_ERROR!>s<!>()
    val res3 = <!EXPERIMENTAL_API_USAGE_ERROR!>res1<!> <!EXPERIMENTAL_API_USAGE_ERROR!>><!> <!EXPERIMENTAL_API_USAGE_ERROR!>res2<!>
    <!EXPERIMENTAL_API_USAGE_ERROR, EXPERIMENTAL_API_USAGE_ERROR, EXPERIMENTAL_API_USAGE_ERROR, EXPERIMENTAL_API_USAGE_ERROR!>for (c in a) {}<!>
}
