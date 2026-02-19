// FIR_IDENTICAL
// FILE: one/two/ClassWithExternalAnnotatedMembers.java
package one.two;

import org.jetbrains.annotations.NotNull;

public class ClassWithExternalAnnotatedMembers {
    public String externalNotNullField;

    @NotNull
    public String explicitNotNullField;

    public static String staticExternalNotNullField;

    @NotNull
    public static String staticExplicitNotNullField;
}

// FILE: one/usage.kt
package one

import one.two.ClassWithExternalAnnotatedMembers

fun test() {
    val x = ClassWithExternalAnnotatedMembers()
    x.externalNotNullField<!UNNECESSARY_SAFE_CALL!>?.<!>foo()
    x.explicitNotNullField<!UNNECESSARY_SAFE_CALL!>?.<!>foo()

    ClassWithExternalAnnotatedMembers.staticExternalNotNullField<!UNNECESSARY_SAFE_CALL!>?.<!>foo()
    ClassWithExternalAnnotatedMembers.staticExplicitNotNullField<!UNNECESSARY_SAFE_CALL!>?.<!>foo()
}

fun String.foo() {

}

// FILE: one/two/annotations.xml
<root>
    <item name='one.two.ClassWithExternalAnnotatedMembers externalNotNullField'>
        <annotation name='org.jetbrains.annotations.NotNull'/>
    </item>
    <item name='one.two.ClassWithExternalAnnotatedMembers staticExternalNotNullField'>
        <annotation name='org.jetbrains.annotations.NotNull'/>
    </item>
</root>