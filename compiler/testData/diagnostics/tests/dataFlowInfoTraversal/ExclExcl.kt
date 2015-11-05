fun bar(x: Int) = x + 1

fun f1(x: Int?) {
    bar(<!TYPE_MISMATCH!>x<!>)
    if (x != null) bar(x<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>)
    if (x == null) <!UNREACHABLE_CODE!>bar(<!><!DEBUG_INFO_SMARTCAST!>x<!>!!<!UNREACHABLE_CODE!>)<!>
}

fun f2(x: Int?) {    
    if (x != null) else <!DEBUG_INFO_SMARTCAST!>x<!>!!
}

fun f3(x: Int?) {    
    if (x != null) bar(x<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>) else <!DEBUG_INFO_SMARTCAST!>x<!>!!
}
    
fun f4(x: Int?) {    
    if (x == null) <!DEBUG_INFO_SMARTCAST!>x<!>!! else bar(x<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>)
}

fun f5(x: Int?) {    
    if (x == null) else bar(x<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>)
}
