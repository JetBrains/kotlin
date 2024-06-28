// FILE: JavaBaseClass.java
public class JavaBaseClass {
    public String getMissingField() {
        return "1";
    }

    private Int privateField = 1;

    public String getPrivateField() {
        return "1";
    }

}

// FILE: EmptySubclass.java

public class EmptySubclass extends JavaBaseClass {
}

// FILE: KotlinSubclassOfJavaGetterOverrides.kt

class KotlinSubclassOfJavaGetterOverrides : JavaBaseClass() {
    fun consumeInt(x: Int) {}
    fun consumeString(x: String) {}

    init {
        consumeString(super.missingField)
        consumeInt(<!ARGUMENT_TYPE_MISMATCH!>super.missingField<!>)
    }

    override fun getMissingField(): String {
        return "1"
    }

    init {
        consumeString(super.privateField)
        consumeInt(<!ARGUMENT_TYPE_MISMATCH!>super.privateField<!>)
    }

    override fun getPrivateField(): String {
        return "1"
    }

}

class KotlinSubclassOfJavaSubclassGetterOverridesInBase : EmptySubclass() {
    fun consumeInt(x: Int) {}
    fun consumeString(x: String) {}

    init {
        consumeString(super.missingField)
        consumeInt(<!ARGUMENT_TYPE_MISMATCH!>super.missingField<!>)
    }

    override fun getMissingField(): String {
        return "1"
    }

    init {
        consumeString(super.privateField)
        consumeInt(<!ARGUMENT_TYPE_MISMATCH!>super.privateField<!>)
    }

    override fun getPrivateField(): String {
        return "1"
    }
}
