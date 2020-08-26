
import kotlin.contracts.*

infix fun Any.mustDo(action: Any) {}
fun initializationOf(target: Any, property: Any, kind: InvocationKind): Any = target
fun initializes(property: Any, kind: InvocationKind) {}
fun invocationOf(target: Any, function: Any, kind: InvocationKind): Any = target
fun invokes(function: Any, kind: InvocationKind) {}
fun receiverOf(lambda: Any): Any = lambda

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class SafeBuilder

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class NotConstruction

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Build

@SafeBuilder
class Builder {

    fun a() {}

    @Build
    fun build() {}
}

fun Builder.myA() {
    contract {
        invokes(this@myA::a, InvocationKind.EXACTLY_ONCE)
    }
    a()
}

<!FUNCTION_INVOCATION_REQUIRED!>fun Builder.myA0() {
    contract {
        invokes(this@myA0::a, InvocationKind.EXACTLY_ONCE)
    }
}<!>

<!FUNCTION_INVOCATION_REQUIRED!>fun Builder.myA1() {
    contract {
        invokes(this@myA1::a, InvocationKind.EXACTLY_ONCE)
    }
    if (true) {
        a()
    }
}<!>

fun Builder.myA2() {
    contract {
        invokes(this@myA2::a, InvocationKind.EXACTLY_ONCE)
    }
    if (true) {
        a()
    } else {
        myA()
    }
}

fun test() {
    val builder = Builder()
    builder.myA()
    builder.build()
}