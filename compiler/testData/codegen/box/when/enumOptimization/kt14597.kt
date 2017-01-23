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