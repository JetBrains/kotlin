enum class En { A, B, С }

fun box() {
    var r = ""

    val en: En = En.A
    when (en) {
        En.A -> { r = "when-1" }
        En.B -> {}
        En.С -> {}
    }

    when (en as En) {
        En.A -> { r = "when-2" }
        En.B -> {}
        En.С -> {}
    }


    // nullable variable
    val en2: Any? = En.A
    if (en2 is En) {
        when (en2) {
            En.A -> { r = "when-3" }
            En.B -> {}
            En.С -> {}
        }

        when (en2 as En) {
            En.A -> { r = "when-4" }
            En.B -> {}
            En.С -> {}
        }
    }


    // not nullable variable
    val en1: Any = En.A
    if (en1 is En) {
        when (en1) {
            En.A -> { r = "when-5" }
            En.B -> {}
            En.С -> {}
        }
        // Working without other examples
        when (en1 as En) {
            En.A -> { r = "when-6" }
            En.B -> {}
            En.С -> {}
        }
    }
}

// 6 TABLESWITCH