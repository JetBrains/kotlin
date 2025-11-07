fun computeFoo(): String = if (foo(0) == 1) "OK" else "FAIL"
fun computeBar(a: A): String = if (a.bar(0) == 1) "OK" else "FAIL"
fun computeBaz(): String = if (B(0).baz == 1) "OK" else "FAIL"