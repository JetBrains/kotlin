// MODULE: common
class Some {
    val e: SomeEnum? = null
}

enum class SomeEnum {
    A, B
}

// MODULE: main()()(common)
fun Some.test() {
    if (e == null) return
    val x = when (<!DEBUG_INFO_SMARTCAST!>e<!>) {
        SomeEnum.A -> "a"
        SomeEnum.B -> "B"
    }
}
