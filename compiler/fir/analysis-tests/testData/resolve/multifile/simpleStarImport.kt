// LANGUAGE: +MultiPlatformProjects

// FILE: B.kt

package b.d

expect interface Other

expect class Another

fun baz() {}

// FILE: A.kt

package a.d

import b.d.*

<!NON_MEMBER_FUNCTION_NO_BODY!>fun foo(arg: Other): Another<!>

fun bar() {
    baz()
}
