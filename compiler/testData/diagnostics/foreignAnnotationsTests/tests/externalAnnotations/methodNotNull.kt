// FIR_IDENTICAL
// FILE: ClassWithExternalAnnotatedMembers.java
import org.jetbrains.annotations.NotNull;

public class ClassWithExternalAnnotatedMembers {
    public String externalNotNullMethod() {
        return "";
    }

    @NotNull
    public String explicitNotNullMethod() {
        return "";
    }

    public static String staticExternalNotNullMethod() {
        return "";
    }

    @NotNull
    public static String staticExplicitNotNullMethod() {
        return "";
    }
}

// FILE: usage.kt
fun test() {
    val x = ClassWithExternalAnnotatedMembers()
    x.externalNotNullMethod()<!UNNECESSARY_SAFE_CALL!>?.<!>foo()
    x.explicitNotNullMethod()<!UNNECESSARY_SAFE_CALL!>?.<!>foo()

    ClassWithExternalAnnotatedMembers.staticExternalNotNullMethod()<!UNNECESSARY_SAFE_CALL!>?.<!>foo()
    ClassWithExternalAnnotatedMembers.staticExplicitNotNullMethod()<!UNNECESSARY_SAFE_CALL!>?.<!>foo()
}

fun String.foo() {

}

// FILE: annotations.xml
<root>
    <item name='ClassWithExternalAnnotatedMembers java.lang.String externalNotNullMethod()'>
        <annotation name='org.jetbrains.annotations.NotNull'/>
    </item>
    <item name='ClassWithExternalAnnotatedMembers java.lang.String staticExternalNotNullMethod()'>
        <annotation name='org.jetbrains.annotations.NotNull'/>
    </item>
</root>