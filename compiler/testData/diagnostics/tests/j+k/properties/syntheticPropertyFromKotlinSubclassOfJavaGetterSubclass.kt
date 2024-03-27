// FILE: JavaBaseClassGetter.java

public class JavaBaseClassGetter {
    public int getPublicField() {
        return 1;
    }

    public int getProtectedField() {
        return 2;
    }

    public int getPrivateField() {
        return 3;
    }
}

// FILE: JavaSubclassOfGetter.java

public class JavaSubclassOfGetter extends JavaBaseClassGetter {
    public String publicField = "";

    protected String protectedField = "";

    private String privateField = "";
}

// FILE: KotlinSubclassOfJavaGetterSubclass.kt

class KotlinSubclassOfJavaGetterSubclass : JavaSubclassOfGetter() {

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