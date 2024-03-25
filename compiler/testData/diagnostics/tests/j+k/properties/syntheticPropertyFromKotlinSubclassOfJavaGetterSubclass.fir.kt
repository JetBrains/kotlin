// FILE: JavaBaseClassGetter.java

public class JavaBaseClassGetter {
    public String getPublicField() {
        return "public field getter of the base class";
    }

    public String getProtectedField() {
        return "protected field getter of the base class";
    }

    public String getPrivateField() {
        return "private field getter of the base class";
    }
}

// FILE: JavaSubclassOfGetter.java

public class JavaSubclassOfGetter extends JavaBaseClassGetter {
    public String publicField = "public field";

    protected String protectedField = "protected field";

    private String privateField = "private field";
}

// FILE: KotlinSubclassOfJavaGetterSubclass.kt

class KotlinSubclassOfJavaGetterSubclass : JavaSubclassOfGetter() {
    fun testPublicField() {
        super.publicField;
    }

    fun testProtectedField() {
        super.protectedField;
    }

    fun testPrivateField() {
        super.privateField;
    }
}