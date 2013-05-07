// "Remove parameter 'x'" "true"
// DISABLE-ERRORS

fun foo(y: Int) {
    foo();
    foo();
    foo(2);
    foo(3);
}