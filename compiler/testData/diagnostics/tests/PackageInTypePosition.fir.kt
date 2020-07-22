// !DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: a.kt

package foo

// FILE: b.kt

@<!OTHER_ERROR!>foo<!> fun bar(p: <!OTHER_ERROR!>foo<!>): <!OTHER_ERROR!>foo<!> = null!!