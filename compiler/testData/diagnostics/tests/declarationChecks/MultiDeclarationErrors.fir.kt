package a

class MyClass {
    fun component1(i: Int) {}
}

class MyClass2 {}

<!CONFLICTING_OVERLOADS!>fun MyClass2.component1()<!> = 1.2
<!CONFLICTING_OVERLOADS!>fun MyClass2.component1()<!> = 1.3

fun test(mc1: MyClass, mc2: MyClass2) {
    val (a, b) = <!COMPONENT_FUNCTION_MISSING!>mc1<!>
    val (c) = <!COMPONENT_FUNCTION_AMBIGUITY!>mc2<!>

    //a,b,c are error types
    use(a, b, c)
}

fun use(vararg a: Any?) = a
