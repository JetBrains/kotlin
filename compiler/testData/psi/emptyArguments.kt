// COMPILATION_ERRORS

fun simple() {
    foo(x, ,)
    foo(,)
    foo(, ,)
    foo(1, , 2,)
}

fun named() {
    foo(x =,)
    foo(x =)
    foo(1, y = )
    foo(x = , y = ,)
    foo(x = , y = 2)
}

fun spread() {
    foo(*)
    foo(*,)
    foo(1, *)
    foo(*, 2)
}

fun mixed() {
    foo(1, x = *,)
    foo(x = , ,)
    foo(x = , *,)
    foo(*, ,)
    foo(*, x = 2, ,)
}
