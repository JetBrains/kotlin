object Main {
    fun test() {
        val type1 = Main
        val type = Main
        if (type === CONST1 || type == CONST2 && type === CONST3) return
        if (type === CONST1 || type == CONST2 && (type === CONST3)) return
        if (type === CONST1 || type == CONST2 && !(type === CONST3)) return
        if (type === CONST1 || type1 == CONST2 && type === CONST3) return
        if (type === CONST1 || type1 == CONST1 && type === CONST3) return
    }

    val CONST1 = Main;
    val CONST2 = Main;
    val CONST3 = Main;
}