// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74928

// FILE: GenericExtensionProperty.kt
interface GenericExtensionProperty {
    val <T> T.prop : String
}

// FILE: JavaClass.java
public class JavaClass implements GenericExtensionProperty {
    @Override
    public <T> String getProp(T t) {
        return "";
    }
}

// FILE: Test.kt
<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class Test<!> : JavaClass()
