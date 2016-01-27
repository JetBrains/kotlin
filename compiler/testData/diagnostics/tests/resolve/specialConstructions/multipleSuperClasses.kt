// !DIAGNOSTICS: -UNUSED_VARIABLE

interface A
interface B

interface C: A, B
interface D: A, B
interface E: A, B

fun foo(c: C?, d: D?, e: E?) {
    val test1: A? = <!TYPE_MISMATCH!>c ?: d<!> ?: e

    val test2: B? = if (false) if (true) c else d else e

    val test3: A? = when {
        true -> c
        else -> when {
            true -> d
            else -> e
        }
    }

    val test4: B? = when (1) {
        1 -> c
        2 -> d
        else -> e
    }
}