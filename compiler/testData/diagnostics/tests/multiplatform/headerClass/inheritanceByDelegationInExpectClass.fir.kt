// MODULE: m1-common
// FILE: common.kt

interface A

class B : A
<!NO_ACTUAL_FOR_EXPECT!>expect class Foo(b: B) : A by b<!>

<!NO_ACTUAL_FOR_EXPECT!>expect class Bar : A by B()<!>
