// RUN_PIPELINE_TILL: FRONTEND
// WITH_JDK
// JVM_TARGET: 1.8
// SCOPE_DUMP: Child:func

// FILE: ITop.java
public interface ITop {
    public default void func() {}
}

// FILE: ISecondary.java
public interface ISecondary extends ITop {
    public default void func() {
    }
}

// FILE: IChild.java
public interface IChild extends ISecondary, ITop {
}

// FILE: GrandParent.java
public class GrandParent implements ITop {
    public final void func() {
    }
}

// FILE: Parent.java
public class Parent extends GrandParent implements ISecondary {
}

// FILE: main.kt

<!MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>class Intermediate<!> : GrandParent(), IChild

class OtherIntermediate : Parent(), ISecondary

abstract class Child : Parent(), IChild

/* GENERATED_FIR_TAGS: classDeclaration, javaType */
