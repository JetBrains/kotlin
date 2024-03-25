// FILE: JavaInterfaceDefaultGetter.java

public interface JavaInterfaceDefaultGetter {
    default String getPublicField() {
        return "public field from default interface getter";
    };

    default String getProtectedField() {
        return "protected field from default interface getter";
    };

    default String getPrivateField() {
        return "private field from default interface getter";
    };
}

// FILE: JavaClassImplementsInterfaceGetter.java

public class JavaClassImplementsInterfaceGetter implements JavaInterfaceDefaultGetter {
    public String publicField = "public field";
    protected String protectedField = "protected field";
    private String privateField = "private field";
}

// FILE: JavaClassFields.java

public class JavaClassFields {
    public String publicField = "public field";
    protected String protectedField = "protected field";
    private String privateField = "private field";
}


// FILE: KotlinSubclassOfInterfaceGetter.kt

class KotlinSubclassOfInterfaceGetter1 : JavaClassFields(), JavaInterfaceDefaultGetter {
    fun testPublicField() {
        super.publicField
    }

    fun testProtectedField() {
        super.protectedField
    }

    fun testInterfacePrivateField() {
        super<JavaInterfaceDefaultGetter>.privateField
    }
}


class KotlinSubclassOfInterfaceGetter2 : JavaClassImplementsInterfaceGetter() {
    fun testPublicField() {
        super.publicField
    }

    fun testProtectedField() {
        super.protectedField
    }

    fun testPrivateField() {
        super.privateField
    }
}
