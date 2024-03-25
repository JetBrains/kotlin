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
        super.missingField
    }

    fun testMissingBooleanField() {
        super.isMissingBooleanField
    }

    fun testPublicField() {
        super.publicField
    }

    fun testPublicBooleanField() {
        super.isPublicBooleanField
    }

    fun testProtectedField() {
        super.protectedField
    }

    fun testProtectedBooleanField() {
        super.isProtectedBooleanField
    }

    fun testPrivateField() {
        super.privateField
    }

    fun testPrivateBooleanField() {
        super.isPrivateBooleanField
    }


}