fun test(f: () -> Unit) =
        try { f() } catch (e: Exception) { throw e }