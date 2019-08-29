package jvm;

fun <K> select(x: K, y: K): K = x

fun test(d1: DiagnosticWithParameters1<*, *>, d2: DiagnosticWithParameters2<*, *, *>) {
    val res = select(d1.a, d2.b)
}