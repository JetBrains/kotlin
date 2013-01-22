fun foo(x: Int) {}

fun loop(var times : Int) {
   while(times > 0) {
        val u : (value : Int) -> Unit = {
            foo(it)
        }
        u(times--)
   }
}

fun box() : String {
    loop(5)
    return "OK"
}