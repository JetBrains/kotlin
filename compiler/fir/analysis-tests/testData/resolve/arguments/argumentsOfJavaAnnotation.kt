// FILE: Ann.java

public @interface Ann {
    String s();
    int x();
    int y() default 1;
}

// FILE: main.kt

@Ann(x = 10, s = "")
@Ann(<!ARGUMENT_TYPE_MISMATCH!>10<!>, <!ARGUMENT_TYPE_MISMATCH!>""<!>)
@Ann(x = 10, s = "", y = 10)
class A

