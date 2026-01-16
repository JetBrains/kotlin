fun main() {
    var local: C? = C()
    // TODO doesn't work in K2 (stability of local var's is always "captured variable")
    println(<expr>local</expr> != null)
}

class C