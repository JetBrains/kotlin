fun box(): String {
    var encl1 = "fail";
    test {
        {
            {
                encl1 = "OK"
            }()
        }()
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

// JVM_TEMPLATES
// 3 INNERCLASS Kt10259_3Kt\$test\$1 null
// 2 INNERCLASS Kt10259_3Kt\$test\$1\$1
// 2 INNERCLASS Kt10259_3Kt\$box\$\$inlined\$test\$1\s
// 2 INNERCLASS Kt10259_3Kt\$box\$\$inlined\$test\$1\$1\s
// inlined:
// 2 INNERCLASS Kt10259_3Kt\$box\$\$inlined\$test\$1\$1\$lambda\$1\s
// 2 INNERCLASS Kt10259_3Kt\$box\$\$inlined\$test\$1\$1\$lambda\$1\$1\s
// 13 INNERCLASS

// NB JVM_IR generates
//  final static INNERCLASS Kt10259_3Kt$box$1$1 null null
//  public final static INNERCLASS Kt10259_3Kt$test$1 null null
// in Kt10259_3Kt.
// Although Oracle JVM doesn't check for consistency of InnerClasses attributes,
// this behavior is equivalent to javac and seems to be correct.

// JVM_IR_TEMPLATES
// 3 INNERCLASS Kt10259_3Kt\$box\$1\$1\s
// 2 INNERCLASS Kt10259_3Kt\$box\$1\$1\$1\s
// 3 INNERCLASS Kt10259_3Kt\$test\$1\s
// 2 INNERCLASS Kt10259_3Kt\$test\$1\$1\s
// 2 INNERCLASS Kt10259_3Kt\$box\$\$inlined\$test\$1\s
// 2 INNERCLASS Kt10259_3Kt\$box\$\$inlined\$test\$1\$1\s
// 14 INNERCLASS