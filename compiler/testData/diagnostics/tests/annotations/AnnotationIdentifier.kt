// FILE: a.kt

annotation class annotation

// FILE: test/b.kt

package test

<!NOT_AN_ANNOTATION_CLASS!>@test.annotation<!> class annotation

// FILE: other/c.kt

package other

annotation class My

<!NOT_AN_ANNOTATION_CLASS!>@test.annotation<!> class Your

@My class Our