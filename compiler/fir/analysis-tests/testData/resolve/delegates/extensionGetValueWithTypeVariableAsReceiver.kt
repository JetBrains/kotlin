// RUN_PIPELINE_TILL: BACKEND
inline fun <L> runLogged(action: () -> L): L {
    return action()
}

operator fun String.getValue(receiver: Any?, p: Any): String =
    runLogged { this }

val testK by runLogged { "K" }
