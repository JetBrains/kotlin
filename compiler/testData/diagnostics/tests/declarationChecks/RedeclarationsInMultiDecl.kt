class A {
    fun component1() : Int = 1
    fun component2() : Int = 2
}

fun a() {
    val (<!REDECLARATION!>a<!>, <!REDECLARATION!>a<!>) = A()
    val (x, <!REDECLARATION!>y<!>) = A();
    val <!REDECLARATION!>b<!> = 1
    use(b)
    val (<!REDECLARATION!>b<!>, <!REDECLARATION!>y<!>) = A();
}


fun use(a: Any): Any = a