// FIR_COMPARISON
class Some {
    companion object {
        val coProp = 12

        fun coFun = 12
    }

    fun some() {
        val a = co<caret>
    }
}

// EXIST: coProp, coFun