// "Create secondary constructor" "true"

trait T

class A: T {
    constructor(i: Int) {
        //To change body of created constructors use File | Settings | File Templates.
    }
}

fun test() {
    val t: T = A(1)
}