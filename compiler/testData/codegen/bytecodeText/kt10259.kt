fun box(): String {
    var encl1 = "fail"
    var encl2 = "fail"
    test {
        {
            encl1 = "OK"
            {
                encl2 = "OK"
            }()
        }()
    }

    return "OK"
}

inline fun test(s: () -> Unit) {
    s()
}

// JVM_TEMPLATES
// 2 INNERCLASS Kt10259Kt\$box\$\$inlined\$test\$lambda\$1\s
// 2 INNERCLASS Kt10259Kt\$box\$\$inlined\$test\$lambda\$1\$1
// 4 INNERCLASS

// NB: JVM_IR generates 'INNERCLASS Kt10259Kt$box$1$1' in 'Kt10259Kt'.
// Although Oracle JVM doesn't check for consistency of InnerClasses attributes,
// this behavior is equivalent to javac and seems to be correct.

// JVM_IR_TEMPLATES
// 3 INNERCLASS Kt10259Kt\$box\$1\$1\s
// 2 INNERCLASS Kt10259Kt\$box\$1\$1\$1
// 5 INNERCLASS
// 1 class Kt10259Kt\$box\$1\$1\ extends
// 1 class Kt10259Kt\$box\$1\$1\$1 extends