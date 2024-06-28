// MODULE: m1-common
// FILE: common.kt

expect annotation class A1(val x: Int, val y: String = "OK")

expect annotation class A2(val x: Int = 42, val y: String = "OK")

expect annotation class A3(val x: Int, val y: String)

expect annotation class A4(val x: Int = 42, val y: String)

expect annotation class A5(val x: Int = 42, val y: String)

@A1(0)
@A2
@A3(0, "")
@A4(0, "")
@A5(0, "")
fun test() {}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual typealias A1 = J1
actual typealias A2 = J2
actual typealias A3 = J3
actual typealias A4 = J4
actual typealias A5 = J5

// FILE: J1.java

public @interface J1 {
    int x();
    String y();
}

// FILE: J2.java

public @interface J2 {
    int x();
    String y() default "OK";
}

// FILE: J3.java

public @interface J3 {
    int x() default 42;
    String y() default "OK";
}

// FILE: J4.java

public @interface J4 {
    int x();
    String y() default "OK";
}

// FILE: J5.java

public @interface J5 {
    int x() default 239;
    String y() default "OK";
}
