// !DIAGNOSTICS: -UNUSED_PARAMETER

class G<T>

fun foo(x: G<String>): G<Int> {null!!}
fun foo(x: G<Int>): G<String> {null!!}