// MODULE: m1-common
// FILE: common.kt

expect enum class En(x: Int) {
    E1,
    E2(42),
    ;

    <!PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED!>constructor(s: String)<!>
}

expect enum class En2 {
    E1()
}
