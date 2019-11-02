fun foo(x: Any) : Runnable  {
    return (x as Runnable)
}

fun box() : String  {
    val r : <no name provided> = <no name provided>()
    return if (EQEQEQ(foo(r), r)) {
    "OK"
}
else {
    "Fail"
}
}
