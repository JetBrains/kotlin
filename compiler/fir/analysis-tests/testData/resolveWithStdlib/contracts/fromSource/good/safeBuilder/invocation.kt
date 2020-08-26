
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
    fun b() {}

    @Build
    fun build() {}

}

fun Builder.ab() {
    contract {
        invokes(this@ab::a, InvocationKind.EXACTLY_ONCE)
        invokes(this@ab::b, InvocationKind.EXACTLY_ONCE)
    }
    a()
    b()
}

fun build(block: Builder.() -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        block mustDo invocationOf(receiverOf(block), Builder::a, InvocationKind.EXACTLY_ONCE)
        block mustDo invocationOf(receiverOf(block), Builder::b, InvocationKind.AT_MOST_ONCE)
    }
    val builder = Builder()
    builder.block()
    builder.build()
}

fun test() {
    build {
        a()
    }

    build {
        a()
        b()
    }

    build {
        ab()
    }

    build {
        if (true) {
            ab()
        } else {
            a()
        }
    }
}