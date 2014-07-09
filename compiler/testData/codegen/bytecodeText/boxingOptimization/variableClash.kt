
fun bar() {
    var x : Object? = java.lang.Integer.valueOf(1) as Object?

    val y1 : Int = (x as Int?)!!

    x = java.lang.Long.valueOf(1) as Object?

    val y2 : Long = (x as Long?)!!
}

// 2 valueOf
// 1 intValue
// 1 longValue
