fun box(): String =
        "O" + try { "K" } catch (e: Exception) { "oops!" }