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
        }()
    }()
}

// 2 INNERCLASS Kt10259_3Kt\$box\$\$inlined\$test\$1\s
// 2 INNERCLASS Kt10259_3Kt\$box\$\$inlined\$test\$1\$1\s

// JVM_TEMPLATES
// 13 INNERCLASS
// 3 INNERCLASS Kt10259_3Kt\$test\$1 null
// 2 INNERCLASS Kt10259_3Kt\$test\$1\$1
// inlined:
// 2 INNERCLASS Kt10259_3Kt\$box\$\$inlined\$test\$1\$1\$lambda\$1\s
// 2 INNERCLASS Kt10259_3Kt\$box\$\$inlined\$test\$1\$1\$lambda\$1\$1\s

// NB JVM_IR generates
//  final static INNERCLASS Kt10259_3Kt$box$1$lam1$1 null null
//  public final static INNERCLASS Kt10259_3Kt$test$1 null null
// in Kt10259_3Kt.
// Although Oracle JVM doesn't check for consistency of InnerClasses attributes,
// this behavior is equivalent to javac and seems to be correct.

// JVM_IR_TEMPLATES
// 14 INNERCLASS
// 3 INNERCLASS Kt10259_3Kt\$box\$1\$lam1\$1 null null
// 2 INNERCLASS Kt10259_3Kt\$box\$1\$lam1\$1\$lam2\$1 null null
// 2 INNERCLASS Kt10259_3Kt\$box\$\$inlined\$test\$1 null null
// 2 INNERCLASS Kt10259_3Kt\$box\$\$inlined\$test\$1\$1 null null
// 3 INNERCLASS Kt10259_3Kt\$test\$1 null null
// 2 INNERCLASS Kt10259_3Kt\$test\$1\$1 null null

