// FILE: A.java
public @interface A {
    String[] value();
    Class<?> x() default Integer.class;
    int y();
}

// FILE: b.kt
@A("1", "2", "3", y = 1) fun test1() {}

@A("4", y = 2) fun test2() {}

@A(*arrayOf("5", "6"), "7", y = 3) fun test3() {}

@A("1", "2", "3", x = String::class, y = 4) fun test4() {}

@A("4", y = 5) fun test5() {}

@A(*arrayOf("5", "6"), "7", x = Any::class, y = 6) fun test6() {}

@A(y = 7) fun test7() {}

@A("8", "9", "10"<!NO_VALUE_FOR_PARAMETER!>)<!> fun test8() {}
