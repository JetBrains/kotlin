// FIR_IDENTICAL
// MODULE: javaModule
// FILE: one/two/FirstModuleClass.java
package one.two;

import org.jetbrains.annotations.NotNull;

public class FirstModuleClass {
    public String externalNotNullMethod() {
        return "";
    }

    @NotNull
    public String explicitNotNullMethod() {
        return "";
    }
}
// FILE: one/two/annotations.xml
<root>
    <item name='one.two.FirstModuleClass java.lang.String externalNotNullMethod()'>
        <annotation name='org.jetbrains.annotations.NotNull'/>
    </item>
</root>
// FILE: usage.kt
package usage1

import one.two.FirstModuleClass

fun test() {
    val x = FirstModuleClass()
    x.externalNotNullMethod()<!UNNECESSARY_SAFE_CALL!>?.<!>foo()
    x.explicitNotNullMethod()<!UNNECESSARY_SAFE_CALL!>?.<!>foo()
}

fun String.foo() {

}
// MODULE: javaModule2
// FILE: three/SecondModuleClass.java
package three;

import org.jetbrains.annotations.NotNull;

public class SecondModuleClass {
    public static String staticExternalNotNullMethod() {
        return "";
    }

    @NotNull
    public static String staticExplicitNotNullMethod() {
        return "";
    }
}
// FILE: three/annotations.xml
<root>
    <item name='three.SecondModuleClass java.lang.String staticExternalNotNullMethod()'>
        <annotation name='org.jetbrains.annotations.NotNull'/>
    </item>
</root>
// FILE: my/pack/usage.kt
package my.pack

import three.SecondModuleClass

fun test() {
    SecondModuleClass.staticExternalNotNullMethod()<!UNNECESSARY_SAFE_CALL!>?.<!>foo()
    SecondModuleClass.staticExplicitNotNullMethod()<!UNNECESSARY_SAFE_CALL!>?.<!>foo()
}

fun String.foo() {

}
