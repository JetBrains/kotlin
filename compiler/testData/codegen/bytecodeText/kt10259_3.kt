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

// 3 INNERCLASS Kt10259_3Kt\$test\$1 null
// 2 INNERCLASS Kt10259_3Kt\$test\$1\$1
// 2 INNERCLASS Kt10259_3Kt\$box\$\$inlined\$test\$1
// 2 INNERCLASS Kt10259_3Kt\$box\$\$inlined\$test\$lambda\$1
// inlined:
// 2 INNERCLASS Kt10259_3Kt\$box\$\$inlined\$test\$lambda\$lambda\$lambda\$1
// 2 INNERCLASS Kt10259_3Kt\$box\$\$inlined\$test\$lambda\$lambda\$lambda\$lambda\$1
// 13 INNERCLASS