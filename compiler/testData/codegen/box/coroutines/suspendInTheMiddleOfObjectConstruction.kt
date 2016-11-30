class Controller {
    suspend fun suspendHere(): String = suspendWithCurrentContinuation { x ->
        x.resume("K")
        Suspend
    }

    suspend fun suspendWithArgument(v: String): String = suspendWithCurrentContinuation { x ->
        x.resume(v)
        Suspend
    }

    suspend fun suspendWithDouble(v: Double): Double = suspendWithCurrentContinuation { x ->
        x.resume(v)
        Suspend
    }

    // INTERCEPT_RESUME_PLACEHOLDER
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {
    c(Controller()).resume(Unit)
}

class A(val first: String, val second: String) {
    override fun toString() = "$first$second"
}
class B(val first: String, val second: String, val third: String) {
    override fun toString() = "$first$second$third"
}

class C(val first: Long, val second: Double, val third: String) {
    override fun toString() = "$first#$second#$third"
}


fun box(): String {
    var result = "OK"

    builder {
        var local: Any = A("O", suspendHere())

        if (local.toString() != "OK") {
            result = "fail 1: $local"
            return@builder
        }

        local = A(suspendWithArgument("O"), suspendHere())

        if (local.toString() != "OK") {
            result = "fail 2: $local"
            return@builder
        }

        local = B("#", suspendWithArgument("O"), suspendHere())

        if (local.toString() != "#OK") {
            result = "fail 3: $local"
            return@builder
        }

        local = B(suspendWithArgument("#"), "O", suspendHere())

        if (local.toString() != "#OK") {
            result = "fail 4: $local"
            return@builder
        }

        local = B("#", B("", "O", suspendWithArgument("")).toString(), suspendHere())

        if (local.toString() != "#OK") {
            result = "fail 5: $local"
            return@builder
        }

        val condition = local.toString() == "#OK"

        local = B(
                if (!condition) "1" else suspendWithArgument("#"),
                if (condition) suspendWithArgument("O") else "2",
                if (condition) suspendHere() else suspendWithArgument("3"))

        if (local.toString() != "#OK") {
            result = "fail 5: $local"
            return@builder
        }

        local = C(1234567890123L, suspendWithDouble(3.14), suspendWithArgument("OK"))

        if (local.toString() != "1234567890123#3.14#OK") {
            result = "fail 5: $local"
            return@builder
        }
    }

    return result
}
