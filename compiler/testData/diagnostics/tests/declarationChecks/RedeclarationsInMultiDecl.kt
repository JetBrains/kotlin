class A {
    operator fun component1() : Int = 1
    operator fun component2() : Int = 2
}

fun a() {
    val (<!REDECLARATION!>a<!>, <!NAME_SHADOWING, REDECLARATION!>a<!>) = A()
    val (x, <!REDECLARATION!>y<!>) = A();
    val <!REDECLARATION!>b<!> = 1
    use(b)
    val (<!NAME_SHADOWING, REDECLARATION!>b<!>, <!NAME_SHADOWING, REDECLARATION!>y<!>) = A();
}


fun use(a: Any): Any = a
