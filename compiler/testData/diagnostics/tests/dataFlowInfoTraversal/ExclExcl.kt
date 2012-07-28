fun bar(x: Int) = x + 1

fun f1(x: Int?) {
    bar(<!TYPE_MISMATCH!>x<!>)
    if (x != null) bar(x<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>)
    if (x == null) bar(x!!)
}

fun f2(x: Int?) {    
    if (x != null) else bar(x!!)
}

fun f3(x: Int?) {    
    if (x != null) bar(x<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>) else bar(x!!)
}
    
fun f4(x: Int?) {    
    if (x == null) bar(x!!) else bar(x<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>)
}

fun f5(x: Int?) {    
    if (x == null) else bar(x<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>)
}
