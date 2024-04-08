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
        consumeString(<!SUPER_CANT_BE_EXTENSION_RECEIVER!>super<!>.missingField)
        consumeInt(<!TYPE_MISMATCH!><!SUPER_CANT_BE_EXTENSION_RECEIVER!>super<!>.missingField<!>)
    }

    override fun getMissingField(): String {
        return "1"
    }

    init {
        consumeString(<!TYPE_MISMATCH!>super.<!INVISIBLE_MEMBER, MISSING_DEPENDENCY_CLASS!>privateField<!><!>)
        consumeInt(<!TYPE_MISMATCH!>super.<!INVISIBLE_MEMBER, MISSING_DEPENDENCY_CLASS!>privateField<!><!>)
    }

    override fun getPrivateField(): String {
        return "1"
    }

}

class KotlinSubclassOfJavaSubclassGetterOverridesInBase : EmptySubclass() {
    fun consumeInt(x: Int) {}
    fun consumeString(x: String) {}

    init {
        consumeString(<!SUPER_CANT_BE_EXTENSION_RECEIVER!>super<!>.missingField)
        consumeInt(<!TYPE_MISMATCH!><!SUPER_CANT_BE_EXTENSION_RECEIVER!>super<!>.missingField<!>)
    }

    override fun getMissingField(): String {
        return "1"
    }

    init {
        consumeString(<!TYPE_MISMATCH!>super.<!INVISIBLE_MEMBER, MISSING_DEPENDENCY_CLASS!>privateField<!><!>)
        consumeInt(<!TYPE_MISMATCH!>super.<!INVISIBLE_MEMBER, MISSING_DEPENDENCY_CLASS!>privateField<!><!>)
    }

    override fun getPrivateField(): String {
        return "1"
    }
}
