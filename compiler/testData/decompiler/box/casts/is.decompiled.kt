fun foo(x: Any) : Boolean  {
    return (x is Runnable)
}

fun box() : String  {
    val r : <no name provided> = <no name provided>()
    return if ((foo(r) && !foo(42))) {
    "OK"
}
else {
    "Fail"
}
}
