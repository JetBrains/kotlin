// FIR_IDENTICAL
// FILE: A.java
public @interface A {
    String[] value();
}

// FILE: b.kt
@A("1", "2", "3") fun test1() {}

@A("4") fun test2() {}

@A(*arrayOf("5", "6"), "7") fun test3() {}

@A() fun test4() {}

@A fun test5() {}
