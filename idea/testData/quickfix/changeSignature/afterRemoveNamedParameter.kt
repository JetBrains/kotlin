// "Remove parameter 'x'" "true"
// DISABLE-ERRORS

fun foo(y: Int) {
    foo();
    foo(y = 1<caret>);
    foo(2);
    foo(3);
}