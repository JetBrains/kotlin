//KT-2382

interface T {
    final fun foo() = "OK"
}

class S : T { }

fun box(): String = S().foo()
