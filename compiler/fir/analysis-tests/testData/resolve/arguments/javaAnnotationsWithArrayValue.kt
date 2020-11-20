// FILE: Ann.java

public @interface Ann {
    String[] value();
}

// FILE: main.kt

@Ann("a", "b")
fun test_1() {}

<!INAPPLICABLE_CANDIDATE!>@Ann(arrayOf("a", "b"))<!>
fun test_2() {}

@Ann(*arrayOf("a", "b"))
fun test_3() {}
