object Main {
    fun test() {
        val type1 = Main
        val type = Main
        if (type === CONST1 || type == CONST2 && type === CONST3) return
        if (type === CONST1 || type == CONST2 && (type === CONST3)) return
        if (type === CONST1 || type == CONST2 && !(type === CONST3)) return
        if (type === CONST1 || type1 == CONST2 && type === CONST3) return
        if (type === CONST1 || type1 == CONST1 && type === CONST3) return

        val type2: Main? = null
        if (type2 == null || type === type2) return

        val type3: Main? = null
        if (type3 === null || type == type3) return

        val type4: Main? = null
        if (type4 == null || type === type2 || type1 == type) return
    }

    val CONST1 = Main;
    val CONST2 = Main;
    val CONST3 = Main;
}