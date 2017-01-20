fun box(): String =
        "O" +
        try {
            throw Exception("oops!")
        }
        catch (e: Exception) {
            try { "K" } catch (e: Exception) { "2" }
        }