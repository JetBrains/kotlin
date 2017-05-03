fun test(receiver: Any?, fn: Any.(Int, String) -> Unit) =
        receiver?.fn(42, "Hello")