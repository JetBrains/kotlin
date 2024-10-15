// RUN_PIPELINE_TILL: BACKEND
fun test(loop: Boolean) {
    while (loop) {
        try {
            do {
                run<Unit> {
                    val a: String
                    if (loop) {
                        a = ""
                    } else {
                        a = ""
                    }
                }
            } while (loop)
        } catch (e: Exception) {
        }
    }
}
