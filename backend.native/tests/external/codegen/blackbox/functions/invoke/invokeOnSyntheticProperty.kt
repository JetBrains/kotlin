// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// FILE: JavaClass.java

public class JavaClass {
    public String getO() {
        return "O";
    }
}

// FILE: main.kt

//  KT-9522 Allow invoke convention for synthetic property

operator fun String.invoke() = this + "K"

fun box(): String {
    return JavaClass().o();
}
