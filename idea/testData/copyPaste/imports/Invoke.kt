package a

class A {
}

operator fun A.invoke() {
}

<selection>fun f(a: A) {
    a()
}</selection>