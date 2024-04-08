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
        consumeString(<!ARGUMENT_TYPE_MISMATCH!>super@KotlinSubclassOfJavaQualifiers.missingField<!>)
        consumeInt(super@KotlinSubclassOfJavaQualifiers.missingField)
    }

    fun testPrivateField() {
        consumeString(<!ARGUMENT_TYPE_MISMATCH!>super@KotlinSubclassOfJavaQualifiers.privateField<!>)
        consumeInt(super@KotlinSubclassOfJavaQualifiers.privateField)
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
            consumeString(<!ARGUMENT_TYPE_MISMATCH!>super@KotlinSubclassOfJavaQualifiers.privateField<!>)
            consumeInt(super@KotlinSubclassOfJavaQualifiers.privateField)

            consumeString(<!ARGUMENT_TYPE_MISMATCH!>super@Inner.privateField<!>)
            consumeInt(super@Inner.privateField)

            consumeString(<!ARGUMENT_TYPE_MISMATCH!>super.privateField<!>)
            consumeInt(super.privateField)
        }

        fun testMissingField() {
            consumeString(<!ARGUMENT_TYPE_MISMATCH!>super@KotlinSubclassOfJavaQualifiers.missingField<!>)
            consumeInt(super@KotlinSubclassOfJavaQualifiers.missingField)

            consumeString(<!ARGUMENT_TYPE_MISMATCH!>super@Inner.missingField<!>)
            consumeInt(super@Inner.missingField)

            consumeString(<!ARGUMENT_TYPE_MISMATCH!>super.missingField<!>)
            consumeInt(super.missingField)
        }
    }
}
