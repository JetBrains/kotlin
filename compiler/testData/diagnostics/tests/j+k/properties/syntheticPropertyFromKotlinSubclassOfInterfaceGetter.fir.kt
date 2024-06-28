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
        super<JavaInterfaceDefaultGetter>.publicField
        super<JavaClassFields>.publicField
    }

    fun testProtectedField() {
        super<JavaInterfaceDefaultGetter>.protectedField
        super<JavaClassFields>.protectedField
    }

    fun testInterfacePrivateField() {
        super<JavaInterfaceDefaultGetter>.privateField
        super<JavaClassFields>.<!INVISIBLE_REFERENCE!>privateField<!>
    }
}

// FILE: KotlinSubclassOfInterfaceGetter2.kt

class KotlinSubclassOfInterfaceGetter2 : JavaClassImplementsInterfaceGetter() {

    fun consumeInt(x: Int) {}
    fun consumeString(x: String) {}

    fun testPublicField() {
        consumeString(super.publicField)
        consumeInt(<!ARGUMENT_TYPE_MISMATCH!>super.publicField<!>)
    }

    fun testProtectedField() {
        consumeString(super.protectedField)
        consumeInt(<!ARGUMENT_TYPE_MISMATCH!>super.protectedField<!>)
    }

    fun testPrivateField() {
        consumeString(<!ARGUMENT_TYPE_MISMATCH!>super.privateField<!>)
        consumeInt(super.privateField)
    }
}