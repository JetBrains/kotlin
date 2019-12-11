// FILE: a.kt

annotation class annotation

// FILE: test/b.kt

package test

@test.annotation class annotation

// FILE: other/c.kt

package other

annotation class My

@test.annotation class Your

@My class Our