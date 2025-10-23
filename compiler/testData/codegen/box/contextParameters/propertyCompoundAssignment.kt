// LANGUAGE: +ContextParameters
// IGNORE_BACKEND_K1: ANY

class LoggingCounter {
    var operationCounter = 0
}

class A {
    context(counter: LoggingCounter)
    var p: Int
        get(): Int {
            counter.operationCounter++
            return 1
        }
        set(value: Int) {
            counter.operationCounter++
        }
}

fun foo() = A()

fun box(): String {
    val loggingCounter = LoggingCounter()
    with(loggingCounter) {
        foo().p += 1
        foo().p = 1
        foo()?.p = 1
        foo().p
    }
    val operationsTotal = loggingCounter.operationCounter
    return if (operationsTotal == 5) "OK" else "$operationsTotal"
}
