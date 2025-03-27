// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-76316
private abstract class C
<!NOTHING_TO_INLINE!>inline<!> fun f(): Any = object : C() {}
