// IGNORE_BACKEND: JS_IR
fun box(): String =
        "O" + try { throw Exception("oops!") } catch (e: Exception) { "K" }