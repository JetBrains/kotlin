// ISSUE: KT-50550

enum class SomeEnum { A, B}

fun test(x: SomeEnum) {
    when (x) {
        SomeEnum.A -> 1
        @Suppress("deprecation")
        SomeEnum.B -> 2
    }.inc()
}
