class A {
    fun test(b: B) {
        <selection>this.b()</selection>
    }
}

class B() {
    fun A.invoke() {}
}