// MODULE: m1-common
// FILE: common.kt

expect enum class En(x: Int) {
    E1,
    E2(42),
    ;

    constructor(s: String)
}

expect enum class En2 {
    E1()
}
