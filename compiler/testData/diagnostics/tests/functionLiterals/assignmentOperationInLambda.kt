fun test(bal: Array<Int>) {
    var bar = 4

    val a = { bar += 4 }
    a : () -> Unit

    val b = { bar = 4 }
    b : () -> Unit

    val c = { bal[2] = 3 }
    c : () -> Unit

    val d = run { bar += 4 }
    d : Unit
}
fun <T> run(f: () -> T): T = f()