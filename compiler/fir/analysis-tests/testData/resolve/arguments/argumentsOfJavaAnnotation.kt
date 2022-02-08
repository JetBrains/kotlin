// FILE: Ann.java

public @interface Ann {
    String s();
    int x();
    int y() default 1;
}

// FILE: main.kt

@Ann(x = 10, s = "")
<!REPEATED_ANNOTATION!>@Ann(<!ARGUMENT_TYPE_MISMATCH, POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION!>10<!>, <!ARGUMENT_TYPE_MISMATCH, POSITIONED_VALUE_ARGUMENT_FOR_JAVA_ANNOTATION!>""<!>)<!>
<!REPEATED_ANNOTATION!>@Ann(x = 10, s = "", y = 10)<!>
class A

