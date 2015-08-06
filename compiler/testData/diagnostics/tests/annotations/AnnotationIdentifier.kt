// FILE: a.kt

<!NOT_AN_ANNOTATION_CLASS!>annotation<!> class annotation

// FILE: test/b.kt

package test

<!NOT_AN_ANNOTATION_CLASS!>test.annotation<!> class annotation

kotlin.annotation.annotation class realAnnotation

realAnnotation class My

// FILE: other/c.kt

package other

annotation class My

<!NOT_AN_ANNOTATION_CLASS!>test.annotation<!> class Your

kotlin.annotation.annotation class His

My class Our
