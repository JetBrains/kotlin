class MyClass {}

deprecated("Use A instead") fun MyClass.minus(i: MyClass) { }
deprecated("Use A instead") fun MyClass.div(i: MyClass) { }
deprecated("Use A instead") fun MyClass.times(i: MyClass) { }

deprecated("Use A instead") fun MyClass.not() { }
deprecated("Use A instead") fun MyClass.plus() { }

deprecated("Use A instead") fun MyClass.contains(i: MyClass): Boolean { return false }

deprecated("Use A instead") fun MyClass.plusAssign(i: MyClass) { }

deprecated("Use A instead") fun MyClass.equals(i: Any?): Boolean { return false }
deprecated("Use A instead") fun MyClass.compareTo(i: MyClass): Int { return 0 }

fun test() {
    val x1 = MyClass()
    val x2 = MyClass()

    x1 <info descr="'fun minus(i : MyClass)' is deprecated. Use A instead">-</info> x2
    x1 <info descr="'fun div(i : MyClass)' is deprecated. Use A instead">/</info> x2
    x1 <info descr="'fun times(i : MyClass)' is deprecated. Use A instead">*</info> x2

    <info descr="'fun not()' is deprecated. Use A instead">!</info>x1
    <info descr="'fun plus()' is deprecated. Use A instead">+</info>x1

    x1 <info descr="'fun contains(i : MyClass)' is deprecated. Use A instead">in</info> x2
    x1 <info descr="'fun contains(i : MyClass)' is deprecated. Use A instead">!in</info> x2

    x1 <info descr="'fun plusAssign(i : MyClass)' is deprecated. Use A instead">+=</info> x2

    x1 <info descr="'fun equals(i : jet.Any?)' is deprecated. Use A instead">==</info> x2
    x1 <info descr="'fun equals(i : jet.Any?)' is deprecated. Use A instead">!=</info> x2
    x1 <info descr="'fun compareTo(i : MyClass)' is deprecated. Use A instead">></info> x2
}