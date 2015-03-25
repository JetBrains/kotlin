// "Create secondary constructor" "true"

open class A {
    constructor(i: Int) {
        //To change body of created constructors use File | Settings | File Templates.
    }

}

class B: A {
    constructor(): super(1) {

    }
}