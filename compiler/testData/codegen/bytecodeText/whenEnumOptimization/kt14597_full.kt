// IGNORE_BACKEND: JVM_IR
enum class En { A, B, С }

fun main(args: Array<String>) {

}

fun box(): String {
    var res1 = "fail"
    var res2 = "fail2"

    val en: En = En.A
    when (en) {
        En.A -> {res1 = ""}
        En.B -> {}
        En.С -> {}
    }

    when (en as En) {
        En.A -> {res1 += "O"}
        En.B -> {}
        En.С -> {}
    }


    // nullable variable
    val en2: Any? = En.A
    if (en2 is En) {
        when (en2) {
            En.A -> {res1 += "K"}
            En.B -> {}
            En.С -> {}
        }

        when (en2 as En) {
            En.A -> {res2 = ""}
            En.B -> {}
            En.С -> {}
        }
    }


    // not nullable variable
    val en1: Any = En.A
    if (en1 is En) {
        when (en1) {
            En.A -> {res2 += "O"}
            En.B -> {}
            En.С -> {}
        }
        // Working without other examples
        when (en1 as En) {
            En.A -> {res2 += "K"}
            En.B -> {}
            En.С -> {}
        }
    }

    if (res1 != res2) return "different results: $res1 != $res2"
    return res1
}

// 6 TABLESWITCH