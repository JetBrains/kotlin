open class B {
    <caret>constructor(a: Int) {

    }

    constructor(): this(1) {

    }
}

open class A: B {
    constructor(a: Int): super(a) {

    }
}

open class C: B(1) {

}

 fun test() {
     B(1)
 }