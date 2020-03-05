fun bar(x: Int): Int = x + 1

fun foo() {
    val x: Int? = null
    val a = Array<Int>(3, {0})

    if (x != null) bar(a[x]) else bar(<!INAPPLICABLE_CANDIDATE!>a[x]<!>)
    bar(a[if (x == null) 0 else x])
    bar(<!INAPPLICABLE_CANDIDATE!>a[x]<!>)
    
    <!INAPPLICABLE_CANDIDATE!>"123"[x]<!>;
    if (x != null) "123"[x];
}
