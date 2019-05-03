class A() {

    override fun toString(): String {
        return "A"
    }
}


fun box(a: String, b: String) : String {

    val s = a + "1" + "2" + 3 + 4L + b + 5.0 + 6F + '7' + A()

    return "OK"
}

// 1 NEW java/lang/StringBuilder