// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR

class LoggingCounter {
    var operationCounter = 0
}

class A {
    context(LoggingCounter)
    var p: Int
        get(): Int {
            operationCounter++
            return 1
        }
        set(value: Int) {
            operationCounter++
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