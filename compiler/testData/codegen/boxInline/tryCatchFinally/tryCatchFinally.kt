// WITH_STDLIB
// KJS_WITH_FULL_RUNTIME
// JVM_ABI_K1_K2_DIFF: KT-63861

// FILE: 1.kt

class My(val value: Int)

inline fun <T, R> T.performWithFinally(job: (T)-> R, finally: (T) -> R) : R {
    try {
        return job(this)
    } finally {
        return finally(this)
    }
}

inline fun <T, R> T.performWithFailFinally(job: (T)-> R, failJob : (e: RuntimeException, T) -> R, finally: (T) -> R) : R {
    try {
        return job(this)
    } catch (e: RuntimeException) {
        return failJob(e, this)
    } finally {
        return finally(this)
    }
}

inline fun String.toInt2() : Int = this.toInt()

// FILE: 2.kt

fun test1(): Int {

    var res = My(111).performWithFinally<My, Int>(
            {
                1
            }, {
                 it.value
            })
    return res
}

fun test11(): Int {
    var result = -1;
    val res = My(111).performWithFinally<My, Int>(
            {
                try {
                    result = it.value
                    throw RuntimeException("1")
                } catch (e: RuntimeException) {
                    ++result
                    throw RuntimeException("2")
                }
            },
            {
                ++result
            })
    return res
}

fun test2(): Int {
    var res = My(111).performWithFinally<My, Int>(
        {
            throw RuntimeException("1")
        },
        {
            it.value
        })


    return res
}

fun test3(): Int {
    try {
        var result = -1;
        val res = My(111).performWithFailFinally<My, Int>(
                {
                    result = it.value;
                    throw RuntimeException("-1")
                },
                { e, z ->
                    ++result
                    throw RuntimeException("-2")
                },
                {
                    ++result
                })
        return res
    } catch (e: RuntimeException) {
        return e.message?.toInt()!!
    }
}

fun box(): String {
    if (test1() != 111) return "test1: ${test1()}"
    if (test11() != 113) return "test11: ${test11()}"

    if (test2() != 111) return "test2: ${test2()}"

    if (test3() != 113) return "test3: ${test3()}"

    return "OK"
}
