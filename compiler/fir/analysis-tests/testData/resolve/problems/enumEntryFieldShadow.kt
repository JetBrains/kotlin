// FILE: BaseJava.java

public interface BaseJava {
    String x = "";
    String y = "";
}

// FILE: DerivedEnum.kt

enum class DerivedEnum : BaseJava {
    x;

    fun foo() {
        bar(x)
        baz(<!UNRESOLVED_REFERENCE!>y<!>)
        baz(BaseJava.y)
    }
}

fun bar(e: DerivedEnum) {}

fun baz(s: String) {
    DerivedEnum.x
}
