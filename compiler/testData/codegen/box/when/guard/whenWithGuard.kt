// LANGUAGE: +WhenGuards
// IGNORE_BACKEND_K1: ANY
// WITH_STDLIB

enum class Problem {
    CONNECTION, AUTHENTICATION, UNKNOWN
}

sealed interface Status {
    data object Loading: Status
    data class Error(val problem: Problem, val isCritical: Boolean): Status
    data class Ok(val info: List<String>): Status
}

fun render(status: Status): String = when (status) {
    Status.Loading -> "loading"
    is Status.Ok if status.info.isEmpty() -> "no data"
    is Status.Ok -> status.info.joinToString()
    is Status.Error if status.problem == Problem.CONNECTION -> "problems with connection"
    is Status.Error if status.problem == Problem.AUTHENTICATION -> "could not be authenticated"
    else -> "unknown problem"
}

fun box(): String {
    render(Status.Loading).also {
        if (it != "loading") return "FAIL(Status.Loading): $it"
    }
    render(Status.Ok(emptyList())).also {
        if (it != "no data") return "FAIL(Status.Ok): $it"
    }
    render(Status.Ok(listOf("info"))).also {
        if (it != "info") return "FAIL(Status.Ok): $it"
    }
    render(Status.Error(Problem.CONNECTION, isCritical = true)).also {
        if (it != "problems with connection") return "FAIL(Status.Error): $it"
    }
    render(Status.Error(Problem.AUTHENTICATION, isCritical = true)).also {
        if (it != "could not be authenticated") return "FAIL(Status.Error): $it"
    }
    render(Status.Error(Problem.UNKNOWN, isCritical = true)).also {
        if (it != "unknown problem") return "FAIL(Status.Error): $it"
    }
    return "OK"
}
