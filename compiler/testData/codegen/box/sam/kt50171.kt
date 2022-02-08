// TARGET_BACKEND: JVM

// FULL_JDK
// SKIP_JDK6

import java.util.function.DoubleConsumer

var value: Double = 3.14

fun f() = {
    g(::value::set)
}

fun g(consumer: DoubleConsumer) {
    consumer.accept(42.0)
}

fun box(): String {
    f()()
    return if (value == 42.0) "OK" else "Fail"
}
