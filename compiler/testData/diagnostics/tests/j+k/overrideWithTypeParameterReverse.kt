// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: Super.kt
interface Super {
    fun <B : Any?> foo(klass: Class<B & Any>): B
}

// FILE: OtherSuper.kt
interface OtherSuper : Super

// FILE: Sub.java
public class Sub implements Super {
    public <A> A foo(Class<A> klass) {
        return null;
    }
}

// FILE: SubSub.kt
class SubSub : OtherSuper, Sub()
class SubSub2 : Sub(), OtherSuper

/* GENERATED_FIR_TAGS: classDeclaration, dnnType, functionDeclaration, interfaceDeclaration, javaType, nullableType,
typeConstraint, typeParameter */
