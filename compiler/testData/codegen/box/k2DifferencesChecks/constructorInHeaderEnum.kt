// ORIGINAL: /compiler/testData/diagnostics/tests/multiplatform/enum/constructorInHeaderEnum.fir.kt
// WITH_STDLIB
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


fun box() = "OK"
