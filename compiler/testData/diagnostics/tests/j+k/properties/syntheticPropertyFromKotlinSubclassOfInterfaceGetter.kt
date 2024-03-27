// FILE: JavaInterfaceDefaultGetter.java

public interface JavaInterfaceDefaultGetter {
    default int getPublicField() {
        return 1;
    }

    default int getProtectedField() {
        return 2;
    }

    default int getPrivateField() {
        return 3;
    }
}

// FILE: JavaClassImplementsInterfaceGetter.java

public class JavaClassImplementsInterfaceGetter implements JavaInterfaceDefaultGetter {
    public String publicField = "";
    protected String protectedField = "";
    private String privateField = "";
}

// FILE: JavaClassFields.java

public class JavaClassFields {
    public String publicField = "";
    protected String protectedField = "";
    private String privateField = "";
}


// FILE: KotlinSubclassOfInterfaceGetter1.kt

class KotlinSubclassOfInterfaceGetter1 : JavaClassFields(), JavaInterfaceDefaultGetter {
    fun testPublicField() {
        <!SUPER_CANT_BE_EXTENSION_RECEIVER!>super<JavaInterfaceDefaultGetter><!>.publicField
        super<JavaClassFields>.publicField
    }

    fun testProtectedField() {
        <!SUPER_CANT_BE_EXTENSION_RECEIVER!>super<JavaInterfaceDefaultGetter><!>.protectedField
        super<JavaClassFields>.protectedField
    }

    fun testInterfacePrivateField() {
        <!SUPER_CANT_BE_EXTENSION_RECEIVER!>super<JavaInterfaceDefaultGetter><!>.privateField
        super<JavaClassFields>.<!INVISIBLE_MEMBER!>privateField<!>
    }
}

// FILE: KotlinSubclassOfInterfaceGetter2.kt

class KotlinSubclassOfInterfaceGetter2 : JavaClassImplementsInterfaceGetter() {

    fun consumeInt(x: Int) {}
    fun consumeString(x: String) {}

    fun testPublicField() {
        consumeString(super.publicField)
        consumeInt(<!TYPE_MISMATCH!>super.publicField<!>)
    }

    fun testProtectedField() {
        consumeString(super.protectedField)
        consumeInt(<!TYPE_MISMATCH!>super.protectedField<!>)
    }

    fun testPrivateField() {
        consumeString(super.<!INVISIBLE_MEMBER!>privateField<!>)
        consumeInt(<!TYPE_MISMATCH!>super.<!INVISIBLE_MEMBER!>privateField<!><!>)
    }
}