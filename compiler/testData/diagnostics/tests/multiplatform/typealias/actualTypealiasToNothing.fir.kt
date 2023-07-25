// MODULE: m1-common
// FILE: common.kt

<!INCOMPATIBLE_MATCHING{JVM}!>expect class E01<!>
<!INCOMPATIBLE_MATCHING{JVM}!>expect class E02<!>

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

typealias MyNothing = Nothing

<!ACTUAL_TYPE_ALIAS_TO_NOTHING!>actual typealias E01 = Nothing<!>
<!ACTUAL_TYPE_ALIAS_NOT_TO_CLASS!>actual typealias E02 = MyNothing<!>
