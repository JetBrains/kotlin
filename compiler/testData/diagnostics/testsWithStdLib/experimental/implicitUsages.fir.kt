// !USE_EXPERIMENTAL: kotlin.RequiresOptIn

@RequiresOptIn
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.TYPEALIAS)
annotation class Marker

@Marker
interface Some

abstract class User {
    abstract fun createSome(): Some
    fun Some?.onSome() {}
    fun withSome(some: Some? = null) {}

    fun use() {
        val something = createSome()
        val somethingOther: Some = createSome()
        null.onSome()
        withSome()
    }
}

data class DataClass(@property:Marker val x: Int)

fun useDataClass(d: DataClass) {
    // Should have error in both
    d.x
    val (x) = d
}

typealias My = Some

fun my(my: My) {}

fun your(my: Some) {}

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
    val x = listOf(A(), B())
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

val x: String by O

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
    val res1 = s - s
    val res2 = s()
    val res3 = res1 > res2
    for (c in a) {}
}