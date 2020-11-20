// FILE: Ann.java

public @interface Ann {
    String s();
    int x();
    int y() default 1;
}

// FILE: main.kt

@Ann(x = 10, s = "")
<!INAPPLICABLE_CANDIDATE!>@Ann(10, "")<!>
@Ann(x = 10, s = "", y = 10)
class A

