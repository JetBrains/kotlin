// LANGUAGE: -JavaTypeParameterDefaultRepresentationWithDNN
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
public interface OtherSuperGeneric<A> extends Super<A> {
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

open <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class DnnNullableGeneric<!><B : Any?> : SuperGeneric<B> {
    <!NOTHING_TO_OVERRIDE!>override<!> fun foo(klass: Class<B & Any>): B = TODO()
}

<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class DnnNullableSubGeneric<!><B : Any?> : OtherSuperGeneric<B>, DnnNullableGeneric<B>()

open class NullableGeneric<B : Any?> : SuperGeneric<B> {
    override fun foo(klass: Class<B>): B = TODO()
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class NullableSubGeneric<!><B : Any?> : OtherSuperGeneric<B>, NullableGeneric<B>()

open class NotNullGeneric<B : Any> : SuperGeneric<B> {
    override fun foo(klass: Class<B>): B = TODO()
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class NotNullSubGeneric<!><B : Any> : OtherSuperGeneric<B>, NotNullGeneric<B>()

