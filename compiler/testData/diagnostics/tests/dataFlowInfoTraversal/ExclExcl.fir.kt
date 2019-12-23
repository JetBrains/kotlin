fun bar(x: Int) = x + 1

fun f1(x: Int?) {
    bar(x)
    if (x != null) bar(x!!)
    if (x == null) bar(x!!)
}

fun f2(x: Int?) {    
    if (x != null) else x!!
}

fun f3(x: Int?) {    
    if (x != null) bar(x!!) else x!!
}
    
fun f4(x: Int?) {    
    if (x == null) x!! else bar(x!!)
}

fun f5(x: Int?) {    
    if (x == null) else bar(x!!)
}
