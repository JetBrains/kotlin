import kotlin.contracts.*

infix fun Any.mustDo(action: Any) {}
fun initializationOf(target: Any, property: Any, kind: InvocationKind): Any = target
fun initializes(property: Any, kind: InvocationKind) {}
fun invocationOf(target: Any, function: Any, kind: InvocationKind): Any = target
fun invokes(function: Any, kind: InvocationKind) {}
fun receiverOf(lambda: Any): Any = lambda
fun requires(action: Any)

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

    @Build
    fun build() {}
}

fun Builder.init() {
    contract {
        requires(initializationOf(this@init, Builder::x, InvocationKind.EXACTLY_ONCE))
    }
}

fun Builder.otherInit() {
    contract {
        requires(initializationOf(this@otherInit, Builder::x, InvocationKind.AT_MOST_ONCE))
    }
}

fun build(block: Builder.() -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        block mustDo initializationOf(receiverOf(block), Builder::x, InvocationKind.UNKNOWN)
    }
    val builder = Builder()
    builder.block()
    builder.build()
}

fun test() {
    build {
        <!PROPERTY_INITIALIZATION_REQUIRED!>init()<!>
    }

    build {
        x = 0
        init()
    }

    build {
        x = 0
        x = 1
        <!PROPERTY_INITIALIZATION_REQUIRED!>init()<!>
    }

    build {
        otherInit()
    }

    build {
        if (true) {
            x = 0
        }
        otherInit()
    }

    build {
        x = 0
        otherInit()
    }

    build {
        x = 0
        x = 1
        <!PROPERTY_INITIALIZATION_REQUIRED!>otherInit()<!>
    }
}