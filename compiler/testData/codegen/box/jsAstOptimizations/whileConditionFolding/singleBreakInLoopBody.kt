// CHECK_OPTIMIZED_JS
// EXPECT_GENERATED_JS: function=box expect=singleBreakInLoopBody.optimized.js

fun box(): String {
    while (true) break
    do {
        break
    } while (true)
    return "OK"
}
