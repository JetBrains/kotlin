object Constants {
    const val A = 30
    const val B = 40
}

class ClassConstants {
    companion object {
        const val C = 50
    }
}
fun foo(state: Int) {
    when (state) {
        Constants.A -> return
        Constants.B -> return
        ClassConstants.C -> return
        else -> return
    }
}

// 1 LOOKUPSWITCH