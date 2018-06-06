// IGNORE_BACKEND: JS_IR
fun box(): String =
        "O" + try { "K" } catch (e: Exception) { "oops!" }