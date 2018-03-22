fun test(x: Any) =
    x == (if (x !is Double) null!! else x)