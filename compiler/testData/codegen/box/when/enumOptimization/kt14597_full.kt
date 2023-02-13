// CHECK_CASES_COUNT: function=box count=18
// CHECK_IF_COUNT: function=box count=3

enum class En { A, B, C }

fun box(): String {
    var res1 = "fail"
    var res2 = "fail2"

    val en: En = En.A
    when (en) {
        En.A -> {res1 = ""}
        En.B -> {}
        En.C -> {}
    }

    when (en as En) {
        En.A -> {res1 += "O"}
        En.B -> {}
        En.C -> {}
    }


    // nullable variable
    val en2: Any? = En.A
    if (en2 is En) {
        when (en2) {
            En.A -> {res1 += "K"}
            En.B -> {}
            En.C -> {}
        }

        when (en2 as En) {
            En.A -> {res2 = ""}
            En.B -> {}
            En.C -> {}
        }
    }


    // not nullable variable
    val en1: Any = En.A
    if (en1 is En) {
        when (en1) {
            En.A -> {res2 += "O"}
            En.B -> {}
            En.C -> {}
        }
        // Working without other examples
        when (en1 as En) {
            En.A -> {res2 += "K"}
            En.B -> {}
            En.C -> {}
        }
    }

    if (res1 != res2) return "different results: $res1 != $res2"
    return res1
}