class A() {

    override fun toString(): String {
        return "A"
    }
}


fun box() : String {
    var s = "1"
    s += "2" + 3 + 4L + 5.0 + 6F + '7' + A()

    return "OK"
}

// 1 NEW java/lang/StringBuilder