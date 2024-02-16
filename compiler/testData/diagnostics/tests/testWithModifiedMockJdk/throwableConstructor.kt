// !JDK_KIND: MODIFIED_MOCK_JDK
// K2 difference is in accordance with KT-65438 where we defined that members in the "grey list"
// (i.e. neither explicitly visible nor hidden) are hidden in the declaring class and since constructors are not inheritable,
// they're always hidden.

abstract class A : <!DEPRECATION!>Throwable<!>(1.0) {}

fun foo() {
    <!DEPRECATION!>Throwable<!>(1.5)
}
