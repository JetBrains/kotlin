class MyClass {}

Deprecated fun MyClass.minus(i: MyClass) { }
Deprecated fun MyClass.div(i: MyClass) { }
Deprecated fun MyClass.times(i: MyClass) { }

Deprecated fun MyClass.not() { }
Deprecated fun MyClass.plus() { }

Deprecated fun MyClass.contains(i: MyClass): Boolean { return false }

Deprecated fun MyClass.plusAssign(i: MyClass) { }

Deprecated fun MyClass.equals(i: Any?): Boolean { return false }
Deprecated fun MyClass.compareTo(i: MyClass): Int { return 0 }

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