val Int.getter: Int
    get() {
        return this@getter
    }

var Int.setter1: Int = 2
    get() {
        return this@setter1
    }
    set(i: Int) {
        $setter1 = this@setter1
    }

var Int.setter2: Int = 2
    set(i: Int) {
        $setter2 = this@setter2
    }

fun box(): String {
    val i = 1
    if (i.getter != 1) return "getter failed"

    i.setter1 = 2
    if (i.setter1 != 1) return "setter1 failed"
    i.setter2 = 2
    if (i.setter2 != 1) return "setter2 failed"

    return "OK"
}