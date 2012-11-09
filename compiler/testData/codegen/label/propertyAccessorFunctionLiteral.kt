val Int.getter: Int
    get() {
        return {
            this@getter
        }.invoke()
    }

var Int.setter: Int = 1
    set(i: Int) {
        $setter = {
            this@setter
        }.invoke()
    }

fun box(): String {
    val i = 1
    if (i.getter != 1) return "getter failed"

    i.setter = 2
    if (i.setter != 1) return "setter failed"

    return "OK"
}