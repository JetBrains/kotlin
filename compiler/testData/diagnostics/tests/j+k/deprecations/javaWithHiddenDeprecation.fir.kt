// RUN_PIPELINE_TILL: FRONTEND
// FILE: JavaClass.java
@kotlin.Deprecated(message = "class", level = kotlin.DeprecationLevel.HIDDEN)
public class JavaClass {
    @kotlin.Deprecated(message = "constructor", level = kotlin.DeprecationLevel.HIDDEN)
    JavaClass() {

    }
}

// FILE: ClassWithMembers.java
public class ClassWithMembers {
    @kotlin.Deprecated(message = "function", level = kotlin.DeprecationLevel.HIDDEN)
    public statis void function() {

    }

    @kotlin.Deprecated(message = "field", level = kotlin.DeprecationLevel.HIDDEN)
    public static int field = 0;
}

// FILE: main.kt
fun check(j: <!DEPRECATION_ERROR!>JavaClass<!>) {
    <!UNRESOLVED_REFERENCE!>JavaClass<!>()

    ClassWithMembers.<!UNRESOLVED_REFERENCE!>function<!>()
    ClassWithMembers.<!UNRESOLVED_REFERENCE, VARIABLE_EXPECTED!>field<!> = 1
}
