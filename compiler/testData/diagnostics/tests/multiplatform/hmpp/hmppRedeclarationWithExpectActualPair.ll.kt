// MODULE: common
// TARGET_PLATFORM: Common

expect class A

expect class B

class C

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common

actual class A

class <!ACTUAL_MISSING!>B<!>

expect class C

// MODULE: main()()(common, intermediate)

class A

actual class <!ACTUAL_WITHOUT_EXPECT!>B<!>

actual class C
