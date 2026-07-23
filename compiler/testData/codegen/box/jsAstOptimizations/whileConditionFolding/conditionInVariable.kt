// CHECK_OPTIMIZED_JS
// EXPECT_GENERATED_JS: function=box expect=conditionInVariable.optimized.js

fun box(): String {
    var i = 0
    var j = 0
    var k = 0
    var result = ""
    while (i < 3) {
        val a = if (j > 2) break else j - 1
        if (k > 2) break
        ++i
        ++j
        ++k
        result += "a=$a;"
    }
    if (result != "a=-1;a=0;a=1;") return "fail: $result"
    return "OK"
}
