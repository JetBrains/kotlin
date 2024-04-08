// FILE: JavaBaseClass.java

public class JavaBaseClass {
    public int getMissingField() {
        return 1;
    }

    public String publicField = "";
    public int getPublicField() {
        return 2;
    }

    private String privateField = "";
    public int getPrivateField() {
        return 3;
    }

    protected String protectedField = "";
    public int getProtectedField() {
        return 4;
    }

    public Boolean isMissingBooleanField() { return true; }

    public Boolean PublicBooleanField = true;
    public Boolean isPublicBooleanField() { return false; }

    protected Boolean protectedBooleanField = true;
    public Boolean isProtectedBooleanField() { return false; }

    private Boolean PrivateBooleanField = false;
    public Boolean isPrivateBooleanField() { return true; }
}

// FILE: EmptySubclass.java

public class EmptySubclass extends JavaBaseClass {
}

// FILE: KotlinSubclassOfJavaSubclass.kt

class KotlinSubclassOfJavaSubclass : EmptySubclass() {

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

    fun testMissingField() {
        consumeString(<!TYPE_MISMATCH!><!SUPER_CANT_BE_EXTENSION_RECEIVER!>super<!>.missingField<!>)
        consumeInt(<!SUPER_CANT_BE_EXTENSION_RECEIVER!>super<!>.missingField)
    }

    fun testMissingBooleanField() {
        <!SUPER_CANT_BE_EXTENSION_RECEIVER!>super<!>.isMissingBooleanField
    }

    fun testPublicBooleanField() {
        <!SUPER_CANT_BE_EXTENSION_RECEIVER!>super<!>.isPublicBooleanField
    }

    fun testProtectedBooleanField() {
        <!SUPER_CANT_BE_EXTENSION_RECEIVER!>super<!>.isProtectedBooleanField
    }

    fun testPrivateBooleanField() {
        <!SUPER_CANT_BE_EXTENSION_RECEIVER!>super<!>.isPrivateBooleanField
    }
}
