open class MyParentClass {
    public open fun blaBlaBla(bla: Int): Int {
        return bla * 2
    }
}

class MyChildClass : MyParentClass() {
    public override fun blaBlaBla(bla: Int): Int {
        val superCall = super.blaBlaBla(bla)
        val thisCall = this.inverse(superCall)
        return thisCall * 2
    }

    fun inverse(value: Int) = -value
}

fun box(): String {
    val myChildClass = MyChildClass()
    val res = myChildClass.blaBlaBla(3)
    when (res) {
        -12 -> return "OK"
        else -> return "FAIL"
    }
}