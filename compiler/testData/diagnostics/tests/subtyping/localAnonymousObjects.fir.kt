// !DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE
fun test() {
    var x = object {}
    x = <!ASSIGNMENT_TYPE_MISMATCH!>object<!> {}
}