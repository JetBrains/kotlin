// FILE: KotlinFile.kt
fun foo(javaClass: JavaClass) {
    javaClass.URL = javaClass.URL + "/"

    javaClass.<!UNRESOLVED_REFERENCE!>url<!>
    javaClass.<!UNRESOLVED_REFERENCE!>uRL<!>
}

// FILE: JavaClass.java
public class JavaClass {
    public String getURL() { return true; }
    public void setURL(String value) { }
}