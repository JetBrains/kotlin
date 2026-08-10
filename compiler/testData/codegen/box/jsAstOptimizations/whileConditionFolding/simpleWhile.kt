// CHECK_OPTIMIZED_JS
// EXPECT_GENERATED_JS: function=box expect=simpleWhile.optimized.js
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
