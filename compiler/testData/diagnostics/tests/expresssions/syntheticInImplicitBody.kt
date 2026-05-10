// RUN_PIPELINE_TILL: BACKEND
// FILE: Owner.java

public class Owner {
    public String getText() {
        return "";
    }
}

// FILE: User.kt

class User : Owner() {
    fun foo() = text

    override fun getText() = ""
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, javaProperty, javaType, override, stringLiteral */
