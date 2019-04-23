actual class A

// Error: ACTUAL_WITHOUT_EXPECT, becuase we shouldn't see expects through usual dependencies
actual class <!ACTUAL_WITHOUT_EXPECT("Actual class 'B'", "")!>B<!>

// OK: though we can't actualize B, we can see it and use it.
fun useB(<!UNUSED_PARAMETER("b")!>b<!>: B) {
}