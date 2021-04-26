// FILE: foo/Some.java

package foo;

class Some {}

class Another {}

// FILE: main.kt
package foo

fun test() {
    val some = Some()
    val another = <!UNRESOLVED_REFERENCE!>Another<!>()
}
