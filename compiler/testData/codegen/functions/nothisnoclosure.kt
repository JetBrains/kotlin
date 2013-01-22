fun loop(var times : Int) {
   while(times > 0) {
        val u : (value : Int) -> Unit = {
            System.out?.println(it)
        }
        u(times--)
   }
}

fun box() : String {
    loop(5)
    return "OK"
}