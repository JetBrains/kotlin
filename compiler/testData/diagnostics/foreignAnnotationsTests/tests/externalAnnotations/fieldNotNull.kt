// FIR_IDENTICAL
// FILE: ClassWithExternalAnnotatedMembers.java
import org.jetbrains.annotations.NotNull;

public class ClassWithExternalAnnotatedMembers {
    public String externalNotNullField;

    @NotNull
    public String explicitNotNullField;

    public static String staticExternalNotNullField;

    @NotNull
    public static String staticExplicitNotNullField;
}

// FILE: usage.kt
fun test() {
    val x = ClassWithExternalAnnotatedMembers()
    x.externalNotNullField<!UNNECESSARY_SAFE_CALL!>?.<!>foo()
    x.explicitNotNullField<!UNNECESSARY_SAFE_CALL!>?.<!>foo()

    ClassWithExternalAnnotatedMembers.staticExternalNotNullField<!UNNECESSARY_SAFE_CALL!>?.<!>foo()
    ClassWithExternalAnnotatedMembers.staticExplicitNotNullField<!UNNECESSARY_SAFE_CALL!>?.<!>foo()
}

fun String.foo() {

}

// FILE: annotations.xml
<root>
    <item name='ClassWithExternalAnnotatedMembers externalNotNullField'>
        <annotation name='org.jetbrains.annotations.NotNull'/>
    </item>
    <item name='ClassWithExternalAnnotatedMembers staticExternalNotNullField'>
        <annotation name='org.jetbrains.annotations.NotNull'/>
    </item>
</root>