class MyClass {
    val i = 0
}

@Deprecated("Use A instead") operator fun MyClass.minus(i: MyClass) { i.i }
@Deprecated("Use A instead") operator fun MyClass.div(i: MyClass) { i.i }
@Deprecated("Use A instead") operator fun MyClass.times(i: MyClass) { i.i }

@Deprecated("Use A instead") operator fun MyClass.not() { }
@Deprecated("Use A instead") operator fun MyClass.plus() { }

@Deprecated("Use A instead") operator fun MyClass.contains(i: MyClass): Boolean { i.i; return false }

@Deprecated("Use A instead") operator fun MyClass.plusAssign(i: MyClass) { i.i }

@Deprecated("Use A instead") operator fun MyClass.rangeTo(i: MyClass): IntRange { return IntRange(i.i, i.i) }

fun test() {
    val x1 = MyClass()
    val x2 = MyClass()

    x1 <warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'minus(MyClass): Unit' is deprecated. Use A instead">-</warning> x2
    x1 <warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'div(MyClass): Unit' is deprecated. Use A instead">/</warning> x2
    x1 <warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'times(MyClass): Unit' is deprecated. Use A instead">*</warning> x2

    <warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'not(): Unit' is deprecated. Use A instead">!</warning>x1
    <warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'plus(): Unit' is deprecated. Use A instead">+</warning>x1

    x1 <warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'contains(MyClass): Boolean' is deprecated. Use A instead">in</warning> x2
    x1 <warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'contains(MyClass): Boolean' is deprecated. Use A instead">!in</warning> x2

    x1 <warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'plusAssign(MyClass): Unit' is deprecated. Use A instead">+=</warning> x2

    x1<warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'rangeTo(MyClass): IntRange' is deprecated. Use A instead">..</warning>x2
}

// NO_CHECK_INFOS
// NO_CHECK_WEAK_WARNINGS