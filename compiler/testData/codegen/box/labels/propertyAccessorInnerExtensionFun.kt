// IGNORE_BACKEND_FIR: JVM_IR
val Int.getter: Int
    get() {
        val extFun: Int.() -> Int = {
            this@getter
        }
        return this@getter.extFun()
    }


var Int.setter: Int
    get() = 1
    set(i: Int) {
        val extFun: Int.() -> Int = {
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
