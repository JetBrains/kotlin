// RUN_PIPELINE_TILL: FRONTEND

enum class Status { NEW, IN_PROGRESS, DONE }

fun enumWhenWithElse(s: Status): String =
    when (s) {
        Status.NEW -> "new"
        Status.DONE -> "done"
        else -> "other"
    }