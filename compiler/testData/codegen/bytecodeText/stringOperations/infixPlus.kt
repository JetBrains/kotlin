class A() {

    override fun toString(): String {
        return "A"
    }
}


fun box() : String {

    val s = "1" plus "2" plus 3 plus 4L plus 5.0 plus 6F plus '7' plus A()

    return "OK"
}

/*TODO:
1 NEW java/lang/StringBuilder*/
