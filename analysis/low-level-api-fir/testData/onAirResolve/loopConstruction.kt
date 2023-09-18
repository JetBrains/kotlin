class X
fun f(): X {
    var x = 3
    val y = 4
    /*PLACE*/while(true) {}
}

fun g(): X {
    /*ONAIR*/while (x < 2) {
        x--
        val z = y
    }
}