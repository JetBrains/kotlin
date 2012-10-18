class MyClass {}

deprecated("'fun minus(i : MyClass)' is deprecated") fun MyClass.minus(i: MyClass) { }
deprecated("'fun div(i : MyClass)' is deprecated") fun MyClass.div(i: MyClass) { }
deprecated("'fun times(i : MyClass)' is deprecated") fun MyClass.times(i: MyClass) { }

deprecated("'fun not()' is deprecated") fun MyClass.not() { }
deprecated("'fun plus()' is deprecated") fun MyClass.plus() { }

deprecated("'fun contains(i : MyClass)' is deprecated") fun MyClass.contains(i: MyClass): Boolean { return false }

deprecated("'fun plusAssign(i : MyClass)' is deprecated") fun MyClass.plusAssign(i: MyClass) { }

deprecated("'fun equals(i : jet.Any?)' is deprecated") fun MyClass.equals(i: Any?): Boolean { return false }
deprecated("'fun compareTo(i : MyClass)' is deprecated") fun MyClass.compareTo(i: MyClass): Int { return 0 }

fun test() {
    val x1 = MyClass()
    val x2 = MyClass()

    x1 <info descr="'fun minus(i : MyClass)' is deprecated">-</info> x2
    x1 <info descr="'fun div(i : MyClass)' is deprecated">/</info> x2
    x1 <info descr="'fun times(i : MyClass)' is deprecated">*</info> x2

    <info descr="'fun not()' is deprecated">!</info>x1
    <info descr="'fun plus()' is deprecated">+</info>x1

    x1 <info descr="'fun contains(i : MyClass)' is deprecated">in</info> x2
    x1 <info descr="'fun contains(i : MyClass)' is deprecated">!in</info> x2

    x1 <info descr="'fun plusAssign(i : MyClass)' is deprecated">+=</info> x2

    x1 <info descr="'fun equals(i : jet.Any?)' is deprecated">==</info> x2
    x1 <info descr="'fun equals(i : jet.Any?)' is deprecated">!=</info> x2
    x1 <info descr="'fun compareTo(i : MyClass)' is deprecated">></info> x2
}