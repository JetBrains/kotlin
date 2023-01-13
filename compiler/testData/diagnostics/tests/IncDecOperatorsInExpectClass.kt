// !LANGUAGE: +MultiPlatformProjects
// SKIP_TXT
// Issue: KT-49714

expect <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>class Counter<!> {
    operator fun inc(): Counter
    operator fun dec(): Counter
}

actual typealias <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>Counter<!> = Int
