// JVM_ABI_K1_K2_DIFF: K2 serializes annotation parameter default values (KT-59526).

annotation class Anno(val x: Array<String> = emptyArray())

@Anno fun test1() = 1
@Anno(arrayOf("K")) fun test2() = 2

fun box(): String {
    return if (test1() + test2() == 3) "OK" else "Fail"
}