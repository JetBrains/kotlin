// TARGET_BACKEND: JVM
// SKIP_JDK6
// JVM_TARGET: 1.8

// FILE: JvmElement.java
public interface JvmElement {
    default void accept() {}
}

// FILE: JvmMember.java
public interface JvmMember extends JvmElement {
    default void accept() {}
}

// FILE: PsiJvmMember.java
// Provides a phantom IO
public interface PsiJvmMember extends JvmMember, JvmElement {}

// FILE: PsiTypeParameterListOwner.java
// Must also provide a phantom IO
public interface PsiTypeParameterListOwner extends PsiJvmMember, JvmMember {}

// FILE: JvmClass.java
public interface JvmClass extends JvmMember, JvmElement {
    default void accept() {}
}

// FILE: PsiClass.java
public interface PsiClass extends PsiTypeParameterListOwner, JvmClass {}

// FILE: Main.kt

class K : PsiClass

fun box() = "OK".also { K() }
