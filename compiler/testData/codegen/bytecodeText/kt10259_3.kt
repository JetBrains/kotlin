fun box(): String {
    var encl1 = "fail";
    test {
        val lam1 = {
            val lam2 = { encl1 = "OK" }
            lam2()
        }
        lam1()
    }

    return encl1
}

inline fun test(crossinline s: () -> Unit) {
    {
        {
            s()
        }.let { it() }
    }.let { it() }
}

// 17 INNERCLASS
// 2 INNERCLASS Kt10259_3Kt\$box\$\$inlined\$test\$1\s
// 2 INNERCLASS Kt10259_3Kt\$box\$\$inlined\$test\$1\$1\s
// 3 INNERCLASS Kt10259_3Kt\$box\$1\$lam1\$1 null null
// 2 INNERCLASS Kt10259_3Kt\$box\$1\$lam1\$1\$lam2\$1 null null
// 2 INNERCLASS Kt10259_3Kt\$box\$\$inlined\$test\$1 null null
// 2 INNERCLASS Kt10259_3Kt\$box\$\$inlined\$test\$1\$1 null null
// 3 INNERCLASS Kt10259_3Kt\$test\$1 null null
// 2 INNERCLASS Kt10259_3Kt\$test\$1\$1 null null
// 3 INNERCLASS kotlin.jvm.internal.Ref\$ObjectRef kotlin.jvm.internal.Ref ObjectRef
