// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -JavaTypeParameterDefaultRepresentationWithDNN +AllowDnnTypeOverridingFlexibleType
// FILE: Super.java
public interface Super {
    <A> A foo(Class<A> klass);
}

// FILE: OtherSuper.java
public interface OtherSuper extends Super {
}

// FILE: SuperGeneric.java
public interface SuperGeneric<A> {
     A foo(Class<A> klass);
}

// FILE: OtherSuperGeneric.java
public interface OtherSuperGeneric<A> extends SuperGeneric<A> {
}

// FILE: Sub.kt
open class DnnNullable : Super {
    override fun <B : Any?> foo(klass: Class<B & Any>): B = TODO()
}

class DnnNullableSub : OtherSuper, DnnNullable()

open class Nullable : Super {
    override fun <B : Any?> foo(klass: Class<B>): B = TODO()
}

class NullableSub : OtherSuper, Nullable()

open class NotNull : Super {
    override fun <B : Any> foo(klass: Class<B>): B = TODO()
}

class NotNullSub : OtherSuper, NotNull()

// ---

open class DnnNullableGeneric<B : Any?> : SuperGeneric<B> {
    override fun foo(klass: Class<B & Any>): B = TODO()
}

class DnnNullableSubGeneric<B : Any?> : OtherSuperGeneric<B>, DnnNullableGeneric<B>()

open class NullableGeneric<B : Any?> : SuperGeneric<B> {
    override fun foo(klass: Class<B>): B = TODO()
}

class NullableSubGeneric<B : Any?> : OtherSuperGeneric<B>, NullableGeneric<B>()

open class NotNullGeneric<B : Any> : SuperGeneric<B> {
    override fun foo(klass: Class<B>): B = TODO()
}

class NotNullSubGeneric<B : Any> : OtherSuperGeneric<B>, NotNullGeneric<B>()

