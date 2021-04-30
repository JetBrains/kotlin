// FILE: a.kt

annotation class annotation

// FILE: test/b.kt

package test

@test.<!NOT_AN_ANNOTATION_CLASS!>annotation<!> class annotation

// FILE: other/c.kt

package other

annotation class My

@test.<!NOT_AN_ANNOTATION_CLASS!>annotation<!> class Your

@My class Our
