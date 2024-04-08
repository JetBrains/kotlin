// FILE: JavaBaseClass.java

public class JavaBaseClass {
    public int getMissingField() {
        return 1;
    }

    private String privateField = "";
    public int getPrivateField() {
        return 1;
    }
}

// FILE: KotlinSubclassOfJavaQualifiers.kt

class KotlinSubclassOfJavaQualifiers : JavaBaseClass() {

    fun consumeInt(x: Int) {}
    fun consumeString(x: String) {}

    fun testMissingField() {
        consumeString(<!TYPE_MISMATCH!><!SUPER_CANT_BE_EXTENSION_RECEIVER!>super@KotlinSubclassOfJavaQualifiers<!>.missingField<!>)
        consumeInt(<!SUPER_CANT_BE_EXTENSION_RECEIVER!>super@KotlinSubclassOfJavaQualifiers<!>.missingField)
    }

    fun testPrivateField() {
        consumeString(super@KotlinSubclassOfJavaQualifiers.<!INVISIBLE_MEMBER!>privateField<!>)
        consumeInt(<!TYPE_MISMATCH!>super@KotlinSubclassOfJavaQualifiers.<!INVISIBLE_MEMBER!>privateField<!><!>)
    }

    fun testPrivateFieldViaInnerClass() {
        val inner = Inner()
        inner.testPrivateField()
    }

    fun testMissingFieldViaInnerClass() {
        val inner = Inner()
        inner.testMissingField()
    }

    inner class Inner : JavaBaseClass() {
        fun testPrivateField() {
            consumeString(super@KotlinSubclassOfJavaQualifiers.<!INVISIBLE_MEMBER!>privateField<!>)
            consumeInt(<!TYPE_MISMATCH!>super@KotlinSubclassOfJavaQualifiers.<!INVISIBLE_MEMBER!>privateField<!><!>)

            consumeString(super@Inner.<!INVISIBLE_MEMBER!>privateField<!>)
            consumeInt(<!TYPE_MISMATCH!>super@Inner.<!INVISIBLE_MEMBER!>privateField<!><!>)

            consumeString(super.<!INVISIBLE_MEMBER!>privateField<!>)
            consumeInt(<!TYPE_MISMATCH!>super.<!INVISIBLE_MEMBER!>privateField<!><!>)
        }

        fun testMissingField() {
            consumeString(<!TYPE_MISMATCH!><!SUPER_CANT_BE_EXTENSION_RECEIVER!>super@KotlinSubclassOfJavaQualifiers<!>.missingField<!>)
            consumeInt(<!SUPER_CANT_BE_EXTENSION_RECEIVER!>super@KotlinSubclassOfJavaQualifiers<!>.missingField)

            consumeString(<!TYPE_MISMATCH!><!SUPER_CANT_BE_EXTENSION_RECEIVER!>super@Inner<!>.missingField<!>)
            consumeInt(<!SUPER_CANT_BE_EXTENSION_RECEIVER!>super@Inner<!>.missingField)

            consumeString(<!TYPE_MISMATCH!><!SUPER_CANT_BE_EXTENSION_RECEIVER!>super<!>.missingField<!>)
            consumeInt(<!SUPER_CANT_BE_EXTENSION_RECEIVER!>super<!>.missingField)
        }
    }
}
