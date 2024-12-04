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
