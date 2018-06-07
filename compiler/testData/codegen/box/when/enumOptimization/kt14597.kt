// IGNORE_BACKEND: JS_IR
// CHECK_CASES_COUNT: function=box count=6
// CHECK_IF_COUNT: function=box count=1

enum class En { A, B, С }

fun box(): String {
    var res = ""
    // nullable variable
    val en2: Any? = En.A
    if (en2 is En) {
        when (en2) {
            En.A -> {res += "O"}
            En.B -> {}
            En.С -> {}
        }

        when (en2 as En) {
            En.A -> {res += "K"}
            En.B -> {}
            En.С -> {}
        }
    }

    return res
}