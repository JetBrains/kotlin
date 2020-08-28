// SKIP_TXT
fun bar() {
    suspend {
        return@suspend
    }

    suspend {
        run {
            return@suspend
        }
    }

    suspend l@{
        return@l
    }

    suspend suspend@{
        return@suspend
    }

    val x = suspend@{
        suspend {
            // Might be resolved to outer lambda, but doesn't make sense because suspend-lambdas here is noinline
            return@suspend
        }
    }
}
