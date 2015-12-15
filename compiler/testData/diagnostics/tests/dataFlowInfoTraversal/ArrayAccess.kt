fun bar(x: Int): Int = x + 1

fun foo() {
    val x: Int? = null
    val a = Array<Int>(3, {0})

    if (x != null) bar(a[<!DEBUG_INFO_SMARTCAST!>x<!>]) else bar(a[<!TYPE_MISMATCH, DEBUG_INFO_CONSTANT!>x<!>])
    bar(a[if (x == null) 0 else <!DEBUG_INFO_SMARTCAST!>x<!>])
    bar(a[<!TYPE_MISMATCH!>x<!>])
    
    "123"[<!TYPE_MISMATCH!>x<!>];
    if (x != null) "123"[<!DEBUG_INFO_SMARTCAST!>x<!>];
}
