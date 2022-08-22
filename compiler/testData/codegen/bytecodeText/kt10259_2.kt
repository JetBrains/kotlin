fun box(): String {
    var encl1 = "fail";
    test {
        encl1 = "OK"
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

// JVM_TEMPLATES
// 3 INNERCLASS Kt10259_2Kt\$test\$1 null
// 2 INNERCLASS Kt10259_2Kt\$test\$1\$1
// 2 INNERCLASS Kt10259_2Kt\$box\$\$inlined\$test\$1\s
// 2 INNERCLASS Kt10259_2Kt\$box\$\$inlined\$test\$1\$1
// 9 INNERCLASS

// JVM_IR_TEMPLATES
// 3 INNERCLASS Kt10259_2Kt\$test\$1 null
// 2 INNERCLASS Kt10259_2Kt\$test\$1\$1
// 2 INNERCLASS Kt10259_2Kt\$box\$\$inlined\$test\$1\s
// 2 INNERCLASS Kt10259_2Kt\$box\$\$inlined\$test\$1\$1
// 1 INNERCLASS kotlin.jvm.internal.Ref\$ObjectRef kotlin.jvm.internal.Ref ObjectRef
// 10 INNERCLASS