// FILE: A.kt
package foo

interface A

// FILE: B.kt
package bar

interface B

// FILE: C.kt
package foo

interface C : A, B {
    interface Nested
}
