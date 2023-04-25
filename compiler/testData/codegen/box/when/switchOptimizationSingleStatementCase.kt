// CHECK_CASES_COUNT: function=test1 count=2 TARGET_BACKENDS=JS
// CHECK_CASES_COUNT: function=test1 count=0 IGNORED_BACKENDS=JS
// CHECK_BREAKS_COUNT: function=test1 count=1 TARGET_BACKENDS=JS
// CHECK_BREAKS_COUNT: function=test1 count=0 IGNORED_BACKENDS=JS
// CHECK_IF_COUNT: function=test1 count=0

// CHECK_CASES_COUNT: function=test2 count=2 TARGET_BACKENDS=JS
// CHECK_CASES_COUNT: function=test2 count=0 IGNORED_BACKENDS=JS
// CHECK_BREAKS_COUNT: function=test2 count=1 TARGET_BACKENDS=JS
// CHECK_BREAKS_COUNT: function=test2 count=0 IGNORED_BACKENDS=JS
// CHECK_IF_COUNT: function=test2 count=0

fun test1(v: Int) {
    when (v) {
        1, 2 -> Unit
    }
}

fun test2(v: Int) {
    loop@ while(true) {
        when (v) {
            1, 2 -> break@loop
        }
    }
}

fun box(): String {
    test1(1)
    test2(1)
    return "OK"
}