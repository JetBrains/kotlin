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
    var a = 0
    var b = 0

    fun foo() {}

    @Build
    fun build() {}
}

fun Builder.initA() {
    contract {
        initializes(this@initA::a, InvocationKind.EXACTLY_ONCE)
    }
    this.a = 0
}

<!UNPROVIDED_SAFE_BUILDER_INITIALIZATION, UNPROVIDED_SAFE_BUILDER_INVOCATION!>fun Builder.test1() {
    this.a = 0
    this.foo()
}<!>

<!UNPROVIDED_SAFE_BUILDER_INITIALIZATION, UNPROVIDED_SAFE_BUILDER_INITIALIZATION!>fun Builder.test2() {
    this.a = 0
    this.b = 0
}<!>

<!UNPROVIDED_SAFE_BUILDER_INITIALIZATION!>fun Builder.test3() {
    initA()
}<!>

fun build(block: Builder.() -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        block mustDo initializationOf(receiverOf(block), Builder::a, InvocationKind.EXACTLY_ONCE)
    }
    val builder = Builder()
    builder.block()
    builder.build()
}

<!UNPROVIDED_SAFE_BUILDER_INITIALIZATION!>fun test4(block: Builder.() -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        block mustDo initializationOf(receiverOf(block), Builder::a, InvocationKind.EXACTLY_ONCE)
    }
    val builder = Builder()
    builder.block()
}<!>

<!UNPROVIDED_SAFE_BUILDER_INITIALIZATION, UNPROVIDED_SAFE_BUILDER_INVOCATION!>fun test5() {
    build <!UNPROVIDED_SAFE_BUILDER_INITIALIZATION, UNPROVIDED_SAFE_BUILDER_INVOCATION!>{
        a = 0
        b = 0
        foo()
    }<!>
}<!>

fun test6() {
    val builder = Builder()
    builder.a = 0
    builder.b = 0
}