// FILE: KotlinFile.kt
fun foo(javaClass: JavaClass) {
    javaClass.url = javaClass.url + "/"
    javaClass.htmlFile += "1"

    javaClass.<!UNRESOLVED_REFERENCE!>URL<!>
    javaClass.<!UNRESOLVED_REFERENCE!>uRL<!>
    javaClass.<!UNRESOLVED_REFERENCE!>HTMLFile<!>
    javaClass.<!UNRESOLVED_REFERENCE!>hTMLFile<!>
    javaClass.<!UNRESOLVED_REFERENCE!>htmlfile<!>
}

// FILE: JavaClass.java
public class JavaClass {
    public String getURL() { return true; }
    public void setURL(String value) { }

    public String getHTMLFile() { return true; }
    public void setHTMLFile(String value) { }
}