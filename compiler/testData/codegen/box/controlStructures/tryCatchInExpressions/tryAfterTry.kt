fun box(): String =
        "" +
        try { "O" } catch (e: Exception) { "1" } +
        try { throw Exception("oops!") } catch (e: Exception) { "K" }