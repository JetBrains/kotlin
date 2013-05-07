// "Remove parameter 'y'" "true"
// DISABLE-ERRORS

fun foo(x: Int) {
    foo();
    foo(1<caret>);
    foo(1);
    foo(2);
}