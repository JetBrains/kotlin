// "Create class 'Foo'" "true"

class A {
    class B {
        fun test() = Foo(2, "2")
    }
}

class Foo(i: Int, s: String) {

}
