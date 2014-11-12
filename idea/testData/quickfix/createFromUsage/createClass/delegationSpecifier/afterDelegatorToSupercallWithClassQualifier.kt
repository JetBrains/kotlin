// "Create class 'A'" "true"
package p

class X {
    open class A(i: Int, s: String) {

    }

}

class Foo: X.A(1, "2") {

}