fun foo(x: Unit) = x

fun test() {
    if (false);
    if (true);

    val x = <!IMPLICIT_CAST_TO_UNIT_OR_ANY!>if (false)<!>;
    foo(x)

    val y: Unit = if (false);
    foo(y)

    foo({if (1==1);}())

    return if (true);
}