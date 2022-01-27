// !LANGUAGE: +MultiPlatformProjects
// SKIP_TXT
// Issue: KT-49714

expect class Counter {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun inc(): Counter
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun dec(): Counter
}

actual typealias Counter = Int
