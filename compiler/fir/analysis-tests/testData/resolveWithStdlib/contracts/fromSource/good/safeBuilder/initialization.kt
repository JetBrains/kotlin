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
    var x: Int = 0
    var y: Int = 0

    @Build
    fun build() {}
}

fun build(block: Builder.() -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        block mustDo initializationOf(receiverOf(block), Builder::x, InvocationKind.EXACTLY_ONCE)
        block mustDo initializationOf(receiverOf(block), Builder::y, InvocationKind.AT_LEAST_ONCE)
    }
    val builder = Builder()
    builder.block()
    builder.build()
}

fun Builder.initXY(x: Int, y: Int) {
    contract {
        initializes(this@initXY::x, InvocationKind.EXACTLY_ONCE)
        initializes(this@initXY::y, InvocationKind.EXACTLY_ONCE)
    }
    this.x = x
    this.y = y
}

fun test() {
    build {
        x = 0

        y = 0
        y = 0
    }

    build {
        x = 0
        y = 0
    }

    build {
        initXY(0, 0)
    }

    build {
        initXY(0, 0)
        y = 0
    }
}