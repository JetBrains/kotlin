// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-78879
// LANGUAGE: -AllowCallingJavaOpenSealedClassConstructor -ProperExhaustivenessCheckForJavaOpenSealedClass
// FILE: Sealed.java
public sealed class Sealed permits Sealed.Sub, Sealed.Sub2 {
    public Sealed() {}

    public static final class Sub extends Sealed {
        public Sub() {}
    }

    public static final class Sub2 extends Sealed {
        public Sub2() {}
    }
}

// FILE: SealedAbstract.java
public sealed abstract class SealedAbstract permits SealedAbstract.Sub {
    public Sealed() {}

    public static final class Sub extends SealedAbstract {
        public Sub() {}
    }
}

// FILE: test.kt
fun testWhen1(sealed: Sealed, sealedAbstract: SealedAbstract) {
    <!NO_ELSE_IN_WHEN!>when<!> (sealed) {
        is Sealed.Sub -> {}
    }

    <!MISSING_BRANCH_FOR_NON_ABSTRACT_SEALED_CLASS!>when<!> (sealed) {
        is Sealed.Sub -> {}
        is Sealed.Sub2 -> {}
    }

    when (sealedAbstract) {
        is SealedAbstract.Sub -> {}
    }
}

fun testWhen2(sealed: Sealed, sealedAbstract: SealedAbstract) {
    when (sealed) {
        is Sealed -> {}
    }

    when (sealed) {
        is Sealed.Sub -> {}
        is Sealed -> {}
    }

    when (sealed) {
        is Sealed.Sub -> {}
        is Sealed.Sub2 -> {}
        is Sealed -> {}
    }

    when (sealedAbstract) {
        is SealedAbstract -> {}
    }

    when (sealedAbstract) {
        is SealedAbstract.Sub -> {}
        is SealedAbstract -> {}
    }
}

fun testConstructorCall() {
    <!SEALED_CLASS_CONSTRUCTOR_CALL!>Sealed()<!>
    <!SEALED_CLASS_CONSTRUCTOR_CALL!>SealedAbstract()<!>
}

// FILE: Base.java
public sealed class Base permits A, B {}

// FILE: A.java
public final class A extends Base {}

// FILE: B.java
public sealed class B extends Base permits B.C, B.D {
    public static final class C extends B {}

    public static non-sealed class D extends B {}
}

// FILE: main.kt
fun test_2(base: Base): String {
    return <!MISSING_BRANCH_FOR_NON_ABSTRACT_SEALED_CLASS!>when<!> (base) {
        is A -> "Fail A"
        is B.C -> "K"
        is B.D -> "Fail B.D"
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, javaFunction, javaType, whenExpression, whenWithSubject */
