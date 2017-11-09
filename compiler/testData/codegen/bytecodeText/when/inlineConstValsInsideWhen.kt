const val A = 10
private const val B = 20

object Constants {
    const val C = 30
}

fun foo(state: Int) {
    when (state) {
        A -> return
        B -> return
        Constants.C -> return
        else -> return
    }
}

// 1 LOOKUPSWITCH