// "Add parameter to function 'foo'" "true"
// DISABLE-ERRORS

fun foo(x: Int,
        i: Int) {
    foo(, 4);
    foo(1, 4);
    foo(1, 4);
    foo(2, 4);
}