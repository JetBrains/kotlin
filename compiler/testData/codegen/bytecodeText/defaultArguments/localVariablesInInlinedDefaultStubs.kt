inline fun test(p: String = "OK"): String {
    var x = 1
    return p
}

fun box() : String {
    return test()
}

// No $iv suffix on LVT entries in test and test$default
// 2 LOCALVARIABLE p Ljava/lang/String;
// 2 LOCALVARIABLE x I

// The $iv suffix should be present in box
// 1 LOCALVARIABLE p\$iv Ljava/lang/String;
// 1 LOCALVARIABLE x\$iv I
