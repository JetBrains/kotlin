// FILE: a.kt

annotation class annotation

// FILE: test/b.kt

package test

<!UNRESOLVED_REFERENCE!>@test.annotation<!> class annotation

// FILE: other/c.kt

package other

annotation class My

<!UNRESOLVED_REFERENCE!>@test.annotation<!> class Your

@My class Our
