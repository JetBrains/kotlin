// DIAGNOSTICS: -IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE
// ^^^ Kotlin/JS partially forbids implementing suspend function interfaces and report this diagnostic
// WITH_STDLIB
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

// The coroutine started by `startCoroutineUninterceptedOrReturn` must run on its own frame:
// `completion` here is the continuation of the calling coroutine, which is suspended meanwhile,
// and it must only be resumed with the outcome when the started coroutine completes.

var c: Continuation<String>? = null

suspend fun park(): String = suspendCoroutineUninterceptedOrReturn { x ->
    c = x
    COROUTINE_SUSPENDED
}

suspend fun selfResume(): String = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume("OK")
    COROUTINE_SUSPENDED
}

suspend fun parkAndAppend(): String = park() + "!"

suspend fun String.parkWithReceiver(): String = this + park()

suspend fun returnOk(): String = "OK"

suspend fun throwOk(): String = throw RuntimeException("OK")

fun regularOk(): String = "OK"

fun regularThrowOk(): String = throw RuntimeException("OK")

class Appender(val suffix: String) {
    suspend fun parkAndAppend(): String = park() + suffix
}

class ParkingFunction : suspend () -> String {
    override suspend fun invoke(): String = park() + "!"
}

class NonSuspendingFunction : suspend () -> String {
    override suspend fun invoke(): String = "OK"
}

// Starts a new coroutine whose completion is the continuation of the current coroutine.
suspend fun startInner(inner: suspend () -> String): String = suspendCoroutineUninterceptedOrReturn { cont ->
    inner.startCoroutineUninterceptedOrReturn(cont)
}

suspend fun startInnerWithReceiver(inner: suspend String.() -> String): String = suspendCoroutineUninterceptedOrReturn { cont ->
    inner.startCoroutineUninterceptedOrReturn(">", cont)
}

// Runs `outer` and expects it to complete with `expected` ("Exception: <message>" for a failure).
// If `resumeWith` is not null, the inner coroutine is expected to park and is then resumed with it.
// Returns the failure description or null.
fun check(name: String, expected: String, resumeWith: Result<String>?, outer: suspend () -> String): String? {
    c = null
    var result: Result<String>? = null
    outer.startCoroutine(Continuation(EmptyCoroutineContext) { result = it })

    if (resumeWith != null) {
        if (result != null) return "fail $name: completed before resume: $result"
        val parked = c ?: return "fail $name: not parked"
        // Resumes the inner coroutine; its completion must resume the outer one with the outcome.
        parked.resumeWith(resumeWith)
    }

    val r = result ?: return "fail $name: outer coroutine is not completed"
    val actual = r.fold({ it }, { "Exception: " + it.message })
    return if (actual == expected) null else "fail $name: $actual"
}

fun box(): String {
    val value = Result.success("OK")
    val exception = Result.failure<String>(RuntimeException("OK"))

    // Suspend lambda
    check("lambda/value", "OK!", value) { startInner { park() + "!" } }?.let { return it }
    check("lambda/exception", "Exception: OK", exception) { startInner { park() + "!" } }?.let { return it }
    check("lambda/no suspension", "OK", null) { startInner { "OK" } }?.let { return it }
    check("lambda/throws", "Exception: OK", null) { startInner { throw RuntimeException("OK") } }?.let { return it }
    check("lambda/self-resume", "OK!", null) { startInner { selfResume() + "!" } }?.let { return it }

    // Callable reference to a suspend function, which is a tail call
    check("tail call reference/value", "OK", value) { startInner(::park) }?.let { return it }
    check("tail call reference/exception", "Exception: OK", exception) { startInner(::park) }?.let { return it }
    check("tail call reference/no suspension", "OK", null) { startInner(::returnOk) }?.let { return it }
    check("tail call reference/throws", "Exception: OK", null) { startInner(::throwOk) }?.let { return it }
    check("tail call reference/self-resume", "OK", null) { startInner(::selfResume) }?.let { return it }

    // Callable reference to a suspend function with its own state
    check("reference/value", "OK!", value) { startInner(::parkAndAppend) }?.let { return it }
    check("reference/exception", "Exception: OK", exception) { startInner(::parkAndAppend) }?.let { return it }

    // Class implementing a suspend function type
    check("class/value", "OK!", value) { startInner(ParkingFunction()) }?.let { return it }
    check("class/exception", "Exception: OK", exception) { startInner(ParkingFunction()) }?.let { return it }
    check("class/no suspension", "OK", null) { startInner(NonSuspendingFunction()) }?.let { return it }

    // Suspend lambda with receiver
    check("receiver lambda/value", ">OK", value) { startInnerWithReceiver { this + park() } }?.let { return it }
    check("receiver lambda/exception", "Exception: OK", exception) { startInnerWithReceiver { this + park() } }?.let { return it }
    check("receiver lambda/no suspension", ">OK", null) { startInnerWithReceiver { this + "OK" } }?.let { return it }

    // Callable reference to a suspend extension function
    check("extension reference/value", ">OK", value) { startInnerWithReceiver(String::parkWithReceiver) }?.let { return it }
    check("extension reference/exception", "Exception: OK", exception) { startInnerWithReceiver(String::parkWithReceiver) }?.let { return it }

    // Suspend lambda capturing a local variable
    val suffix = "!"
    check("capturing lambda/value", "OK!", value) { startInner { park() + suffix } }?.let { return it }
    check("capturing lambda/exception", "Exception: OK", exception) { startInner { park() + suffix } }?.let { return it }
    check("capturing lambda/no suspension", "OK!", null) { startInner { "OK" + suffix } }?.let { return it }

    // Bound callable references, which capture the receiver
    val appender = Appender("!")
    check("bound member reference/value", "OK!", value) { startInner(appender::parkAndAppend) }?.let { return it }
    check("bound member reference/exception", "Exception: OK", exception) { startInner(appender::parkAndAppend) }?.let { return it }
    check("bound extension reference/value", ">OK", value) { startInner(">"::parkWithReceiver) }?.let { return it }
    check("bound extension reference/exception", "Exception: OK", exception) { startInner(">"::parkWithReceiver) }?.let { return it }

    // Suspend conversion of non-suspend functions: they cannot suspend
    val regularLambda: () -> String = { "OK" }
    val regularThrowingLambda: () -> String = { throw RuntimeException("OK") }
    check("suspend conversion of lambda/no suspension", "OK", null) { startInner(regularLambda) }?.let { return it }
    check("suspend conversion of lambda/throws", "Exception: OK", null) { startInner(regularThrowingLambda) }?.let { return it }
    check("suspend conversion of reference/no suspension", "OK", null) { startInner(::regularOk) }?.let { return it }
    check("suspend conversion of reference/throws", "Exception: OK", null) { startInner(::regularThrowOk) }?.let { return it }

    return "OK"
}
