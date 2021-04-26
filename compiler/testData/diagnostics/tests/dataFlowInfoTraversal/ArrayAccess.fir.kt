fun bar(x: Int): Int = x + 1

fun foo() {
    val x: Int? = null
    val a = Array<Int>(3, {0})

    if (x != null) bar(a[x]) else bar(a[<!ARGUMENT_TYPE_MISMATCH!>x<!>])
    bar(a[if (x == null) 0 else x])
    bar(a[<!ARGUMENT_TYPE_MISMATCH!>x<!>])

    "123"[<!ARGUMENT_TYPE_MISMATCH!>x<!>];
    if (x != null) "123"[x];
}
