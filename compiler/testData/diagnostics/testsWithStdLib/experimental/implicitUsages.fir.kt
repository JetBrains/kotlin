// !OPT_IN: kotlin.RequiresOptIn
// LANGUAGE: +OptInContagiousSignatures

@RequiresOptIn
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.TYPEALIAS)
annotation class Marker

@Marker
interface Some

abstract class User {
    abstract fun createSome(): <!OPT_IN_USAGE_ERROR!>Some<!>
    fun <!OPT_IN_USAGE_ERROR!>Some<!>?.onSome() {}
    fun withSome(some: <!OPT_IN_USAGE_ERROR!>Some<!>? = null) {}

    fun use() {
        val something = <!OPT_IN_USAGE_ERROR!>createSome<!>()
        val somethingOther: <!OPT_IN_USAGE_ERROR!>Some<!> = <!OPT_IN_USAGE_ERROR!>createSome<!>()
        null.<!OPT_IN_USAGE_ERROR!>onSome<!>()
        <!OPT_IN_USAGE_ERROR!>withSome<!>()
    }
}

data class DataClass(@property:Marker val x: Int)

fun useDataClass(d: DataClass) {
    // Should have error in both
    d.<!OPT_IN_USAGE_ERROR!>x<!>
    val (x) = d
}

typealias My = <!OPT_IN_USAGE_ERROR!>Some<!>

fun my(my: <!OPT_IN_USAGE_ERROR!>My<!>) {}

fun your(my: <!OPT_IN_USAGE_ERROR!>Some<!>) {}

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
    arg.bar()
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
    val x = <!OPT_IN_USAGE_ERROR!>listOf<!>(A(), B())
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

val x: String by <!OPT_IN_USAGE_ERROR, OPT_IN_USAGE_ERROR!>O<!>

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
    val res1 = s <!OPT_IN_USAGE_ERROR!>-<!> s
    val res2 = <!OPT_IN_USAGE_ERROR!>s<!>()
    val res3 = <!OPT_IN_USAGE_ERROR!>res1<!> <!OPT_IN_USAGE_ERROR!>><!> <!OPT_IN_USAGE_ERROR!>res2<!>
    for (c in <!OPT_IN_USAGE_ERROR, OPT_IN_USAGE_ERROR, OPT_IN_USAGE_ERROR, OPT_IN_USAGE_ERROR, OPT_IN_USAGE_ERROR!>a<!>) {}
}
