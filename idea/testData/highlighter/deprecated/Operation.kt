class MyClass {
    val i = 0
}

deprecated("Use A instead") fun MyClass.minus(i: MyClass) { i.i }
deprecated("Use A instead") fun MyClass.div(i: MyClass) { i.i }
deprecated("Use A instead") fun MyClass.times(i: MyClass) { i.i }

deprecated("Use A instead") fun MyClass.not() { }
deprecated("Use A instead") fun MyClass.plus() { }

deprecated("Use A instead") fun MyClass.contains(i: MyClass): Boolean { i.i; return false }

deprecated("Use A instead") fun MyClass.plusAssign(i: MyClass) { i.i }

deprecated("Use A instead") fun MyClass.equals(i: Any?): Boolean { i == null; return false }
deprecated("Use A instead") fun MyClass.compareTo(i: MyClass): Int { return i.i }

fun test() {
    val x1 = MyClass()
    val x2 = MyClass()

    x1 <warning descr="'fun minus(i: MyClass)' is deprecated. Use A instead">-</warning> x2
    x1 <warning descr="'fun div(i: MyClass)' is deprecated. Use A instead">/</warning> x2
    x1 <warning descr="'fun times(i: MyClass)' is deprecated. Use A instead">*</warning> x2

    <warning descr="'fun not()' is deprecated. Use A instead">!</warning>x1
    <warning descr="'fun plus()' is deprecated. Use A instead">+</warning>x1

    x1 <warning descr="'fun contains(i: MyClass)' is deprecated. Use A instead">in</warning> x2
    x1 <warning descr="'fun contains(i: MyClass)' is deprecated. Use A instead">!in</warning> x2

    x1 <warning descr="'fun plusAssign(i: MyClass)' is deprecated. Use A instead">+=</warning> x2

    x1 <warning descr="'fun equals(i: jet.Any?)' is deprecated. Use A instead">==</warning> x2
    x1 <warning descr="'fun equals(i: jet.Any?)' is deprecated. Use A instead">!=</warning> x2
    x1 <warning descr="'fun compareTo(i: MyClass)' is deprecated. Use A instead">></warning> x2
}