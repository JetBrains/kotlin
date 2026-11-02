// CHECK_OPTIMIZED_JS
// EXPECT_GENERATED_JS: function=box expect=simpleWhile.optimized.js IGNORED_MODES=USE_CONST_AND_LET_FOR_VARIABLES
// EXPECT_GENERATED_JS: function=box expect=simpleWhile.optimized.constLet.js TARGET_MODES=USE_CONST_AND_LET_FOR_VARIABLES
fun box(): String {
    var i = 1
    var sum = 0
    while (true) {
        if (i >= 10) {
            break
        }
        sum += i
        i++
    }

    if (sum != 45) return "fail: " + sum

    return "OK"
}
