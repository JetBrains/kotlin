// MODULE: m1-common
// FILE: common.kt
<!NO_ACTUAL_FOR_EXPECT!>expect open class A {
    constructor(s: String)

    constructor(n: Number) : this("A")
}<!>

<!NO_ACTUAL_FOR_EXPECT!>expect class B : A {
    constructor(i: Int)

    constructor() : super("B")
}<!>
