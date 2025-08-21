// RUN_PIPELINE_TILL: FRONTEND
// JAVAC_EXPECTED_FILE
// LANGUAGE: -ForbidInferOfInvisibleTypeAsReifiedVarargOrReturnType, -ForbidExposingPackagePrivateInInternal

// FILE: test/Internal.java

package test;

class Internal {}

// FILE: test/My.java

package test;

public class My {
    static public Internal foo() { return new Internal(); }
}

// FILE: test/His.kt

package test

class His {
    // Ok: private vs package-private
    private fun private() = My.foo()
    // Ok: internal vs package-private in same package
    internal fun <!EXPOSED_PACKAGE_PRIVATE_TYPE_FROM_INTERNAL_WARNING!>internal<!>() = My.foo()
    // Error: protected vs package-private
    protected fun <!EXPOSED_FUNCTION_RETURN_TYPE!>protected<!>() = My.foo()
    // Error: public vs package-private
    fun <!EXPOSED_FUNCTION_RETURN_TYPE!>public<!>() = My.foo()
}

// FILE: other/Your.kt

package other

import test.My

class Your {
    internal fun <!EXPOSED_PACKAGE_PRIVATE_TYPE_FROM_INTERNAL_WARNING!>bar<!>() = <!INFERRED_INVISIBLE_RETURN_TYPE_WARNING!>My.foo()<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, functionDeclaration, javaFunction */
