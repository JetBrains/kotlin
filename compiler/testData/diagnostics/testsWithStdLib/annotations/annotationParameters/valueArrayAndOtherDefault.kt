// FILE: A.java
public @interface A {
    String[] value();
    Class<?> x() default Integer.class;
    int y() default 1;
}

// FILE: b.kt
[A("1", "2", "3")] fun test1() {}

[A("4")] fun test2() {}

[A(*array("5", "6"), "7")] fun test3() {}

[A("1", "2", "3", x = javaClass<String>())] fun test4() {}

[A("4", y = 2)] fun test5() {}

[A(*array("5", "6"), "7", x = javaClass<Any>(), y = 3)] fun test6() {}

[A()] fun test7() {}

[A] fun test8() {}

[A(x = <!TYPE_MISMATCH(kotlin.reflect.KClass<*>; java.lang.Class<kotlin.Any>)!>javaClass<Any>()<!>, <!MIXING_NAMED_AND_POSITIONED_ARGUMENTS!>*array("5", "6")<!>, <!MIXING_NAMED_AND_POSITIONED_ARGUMENTS!>"7"<!>, y = 3)] fun test9() {}
[A(x = <!TYPE_MISMATCH(kotlin.reflect.KClass<*>; java.lang.Class<kotlin.Any>)!>javaClass<Any>()<!>, value = *array("5", "6"), <!MIXING_NAMED_AND_POSITIONED_ARGUMENTS!>"7"<!>, y = 3)] fun test10() {}
[A(x = javaClass<Any>(), value = *array("5", "6", "7"), y = 3)] fun test11() {}
