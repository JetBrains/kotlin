// LAMBDAS: CLASS

fun box(): String {
    var encl1 = "fail"
    var encl2 = "fail"
    test {
        val lam1 = {
            encl1 = "OK"
            val lam2 = { encl2 = "OK" }
            lam2()
        }
        lam1()
    }

    return "OK"
}

inline fun test(s: () -> Unit) {
    s()
}

// JVM_TEMPLATES
// 4 INNERCLASS
// 2 INNERCLASS Kt10259Kt\$box\$\$inlined\$test\$lambda\$1\s
// 2 INNERCLASS Kt10259Kt\$box\$\$inlined\$test\$lambda\$1\$1

// NB: JVM_IR generates 'INNERCLASS Kt10259Kt$box$1$1' in 'Kt10259Kt'.
// Although Oracle JVM doesn't check for consistency of InnerClasses attributes,
// this behavior is equivalent to javac and seems to be correct.

// JVM_IR_TEMPLATES
// 8 INNERCLASS
// 3 INNERCLASS Kt10259Kt\$box\$1\$lam1\$1 null null
// 2 INNERCLASS Kt10259Kt\$box\$1\$lam1\$1\$lam2\$1
// 3 INNERCLASS kotlin.jvm.internal.Ref\$ObjectRef kotlin.jvm.internal.Ref ObjectRef
