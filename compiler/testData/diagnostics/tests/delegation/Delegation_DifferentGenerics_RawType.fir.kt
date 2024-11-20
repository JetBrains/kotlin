// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-46120, KT-72140
// WITH_STDLIB
// RENDER_DIAGNOSTICS_FULL_TEXT
// LANGUAGE: -ForbidImplementationByDelegationWithDifferentGenericSignature

// FILE: JI.java
import java.util.List;

public interface JI {
    <C> List<C> foo();
}

// FILE: JC.java

import java.util.List;

public class JC implements JI {
    @Override
    public List foo() {
        return null;
    }
}

// FILE: JKC.java

import java.util.List;

public class JKC implements KI {
    @Override
    public List foo() {
        return null;
    }
}

// FILE: KI.kt

interface KI {
    fun <T> foo(): List<T>
}

// FILE: test.kt

class C: <!IMPLEMENTATION_BY_DELEGATION_WITH_DIFFERENT_GENERIC_SIGNATURE_WARNING!>JI<!> by JC()

class C2: <!IMPLEMENTATION_BY_DELEGATION_WITH_DIFFERENT_GENERIC_SIGNATURE_WARNING!>KI<!> by JKC()
