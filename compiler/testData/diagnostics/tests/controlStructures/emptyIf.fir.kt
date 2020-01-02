fun foo(x: Unit) = x

fun test() {
    if (false);
    if (true);

    val x = if (false);
    foo(x)

    val y: Unit = if (false);
    foo(y)

    foo(<!UNRESOLVED_REFERENCE!>{if (1==1);}()<!>)

    return if (true);
}