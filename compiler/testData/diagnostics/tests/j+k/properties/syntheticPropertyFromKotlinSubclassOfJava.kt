// FILE: JavaBaseClass.java

public class JavaBaseClass {
    public String getMissingField() {
        return "";
    }

    public String publicField = "";
    public String getPublicField() {
        return "";
    }

    private String privateField = "";
    public String getPrivateField() {
        return "";
    }

    protected String protectedField = "";
    public String getProtectedField() {
        return "";
    }

    public String publicFieldPrivateGetter = "";
    private String getPublicFieldPrivateGetter() {
        return "";
    }

    public Boolean isMissingBooleanField() { return true; }

    public Boolean PublicBooleanField = true;
    public Boolean isPublicBooleanField() { return false; }

    protected Boolean protectedBooleanField = true;
    public Boolean isProtectedBooleanField() { return false; }

    private Boolean PrivateBooleanField = false;
    public Boolean isPrivateBooleanField() { return true; }
}



// FILE: KotlinSubclassOfJava.kt

class KotlinSubclassOfJava : JavaBaseClass() {
    fun testMissingField() {
        <!SUPER_CANT_BE_EXTENSION_RECEIVER!>super<!>.missingField
    }

    fun testMissingBooleanField() {
        <!SUPER_CANT_BE_EXTENSION_RECEIVER!>super<!>.isMissingBooleanField
    }

    fun testPublicField() {
        super.publicField
    }

    fun testPublicBooleanField() {
        <!SUPER_CANT_BE_EXTENSION_RECEIVER!>super<!>.isPublicBooleanField
    }

    fun testProtectedField() {
        super.protectedField
    }

    fun testProtectedBooleanField() {
        <!SUPER_CANT_BE_EXTENSION_RECEIVER!>super<!>.isProtectedBooleanField
    }

    fun testPrivateField() {
        super.<!INVISIBLE_MEMBER!>privateField<!>
    }

    fun testPrivateBooleanField() {
        <!SUPER_CANT_BE_EXTENSION_RECEIVER!>super<!>.isPrivateBooleanField
    }


}