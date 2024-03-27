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



// FILE: KotlinSubclassOfJava.kt

class KotlinSubclassOfJava : JavaBaseClass() {

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

    fun testMissingField() {
        consumeString(<!ARGUMENT_TYPE_MISMATCH!>super.missingField<!>)
        consumeInt(super.missingField)
    }

    fun testMissingBooleanField() {
        super.isMissingBooleanField
    }

    fun testPublicBooleanField() {
        super.isPublicBooleanField
    }

    fun testProtectedBooleanField() {
        super.isProtectedBooleanField
    }

    fun testPrivateBooleanField() {
        super.isPrivateBooleanField
    }


}