open class X(s : String) {
    public var n: String = s
        private set

}

class Z : X("subclass") {
    fun print(): String {
        <!INVISIBLE_SETTER!>n<!> = n
        return n;
    }
}


fun box() : String {
    return Z().print() //error
}