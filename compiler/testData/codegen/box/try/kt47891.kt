// TARGET_BACKEND: JVM

fun box(): String {
    try {
        try {
            run {
                return "NOT OK1"
            }
        } finally {
            return "NOT OK2"
        }
    } finally {
        return "OK"
    }
}
