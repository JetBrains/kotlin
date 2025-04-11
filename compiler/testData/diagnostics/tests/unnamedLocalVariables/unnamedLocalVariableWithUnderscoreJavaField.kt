// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74809
// LANGUAGE: +UnnamedLocalVariables
// TARGET_BACKEND: JVM
// JDK_KIND: FULL_JDK

// FILE: JavaClassWithField.java
public class JavaClassWithField {
    public String _ = "1";
}

// FILE: JavaClassWithSyntheticProperty.java
public class JavaClassWithSyntheticProperty {
    public String get_(){
        return "1";
    }
    public String foo(String _){
        return "2";
    }
}

// FILE: test.kt
import JavaClassWithField
import JavaClassWithSyntheticProperty

class Derived: JavaClassWithSyntheticProperty() {
    override fun get_(): String {
        val <!UNDERSCORE_IS_RESERVED!>_<!> : String = "2"
        <!SUPER_CANT_BE_EXTENSION_RECEIVER!>super<!>.<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!>
        return <!SUPER_CANT_BE_EXTENSION_RECEIVER!>super<!>.`_`
    }

    override fun foo(`_`: String?): String? {
        return super.foo(`_`)
    }
}

class Derived2: JavaClassWithSyntheticProperty() {
    override fun foo(<!UNDERSCORE_IS_RESERVED!>_<!>: String?): String? {
        return super.foo(<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!>)
    }
}

fun test() {
    JavaClassWithField().<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!>
    JavaClassWithField().`_`
    JavaClassWithSyntheticProperty().<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!>
    JavaClassWithSyntheticProperty().`_`
    JavaClassWithSyntheticProperty().foo(<!NAMED_ARGUMENTS_NOT_ALLOWED!>_<!> = "")
    Derived().<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!>
    Derived().`_`
}
