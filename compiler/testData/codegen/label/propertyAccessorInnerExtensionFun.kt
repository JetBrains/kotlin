val Int.getter: Int
    get() {
        val extFun = { Int.() ->
            this@getter
        }
        return this@getter.extFun()
    }


var Int.setter: Int = 1
    set(i: Int) {
        val extFun = { Int.() ->
            this@setter
        }
        this@setter.extFun()
    }


fun box(): String {
    val i = 1
    if (i.getter != 1) return "getter failed"

    i.setter = 1
    if (i.setter != 1) return "setter failed"

    return "OK"
}