tailrec fun foo(n: Int = 10): Int =
    if (n <= 0) 0 else foo(n - 1)