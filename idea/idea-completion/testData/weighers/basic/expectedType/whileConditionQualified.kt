// FIR_COMPARISON

object x {
    val b: Boolean = true
    val c: String = "true"
    val d: Int = 1
    val e: Long = 1L
    val f: Boolean = true

    fun foo(): Boolean = true
}


fun x() {
    while(x.<caret>){
}

// ORDER: b, f, foo