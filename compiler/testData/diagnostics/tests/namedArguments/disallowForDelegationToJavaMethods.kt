// FIR_IDENTICAL
// SKIP_TXT

// FILE: JavaInterface.java

public interface JavaInterface {
    public void foo(int javaName);
}

// FILE: JavaClass.java

public class JavaSuperClass implements JavaInterface {
    @Override
    public void foo(int javaName) {}
}

// FILE: 1.kt

class KtClass: JavaInterface by JavaSuperClass()

fun test() {
    val ktInstance = KtClass()
    ktInstance.foo(<!NAMED_ARGUMENTS_NOT_ALLOWED!>javaName<!> = 1)
}
