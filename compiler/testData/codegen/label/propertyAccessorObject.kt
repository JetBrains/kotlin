trait Base {
    fun foo(): Int
}

val Int.getter: Int
    get() {
        return object: Base {
            override fun foo(): Int {
                return this@getter
            }
        }.foo()
    }

var Int.setter1: Int = 2
    set(i: Int) {
        $setter1 = object: Base {
            override fun foo(): Int {
                this@setter1
                return 1
            }
        }.foo()
    }


var Int.setter2: Int = 2
    get() {
        return object: Base {
            override fun foo(): Int {
                return this@setter2
            }
        }.foo()
    }
    set(i: Int) {
        $setter2 = object: Base {
            override fun foo(): Int {
                this@setter2
                return 1
            }
        }.foo()
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