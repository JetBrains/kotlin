// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-46120, KT-72140
// LANGUAGE: -ForbidImplementationByDelegationWithDifferentGenericSignature

// FILE: JI.java

public interface JI {
    <C> C foo();
}

// FILE: JC.java

public class JC implements JI {
    @Override
    public String foo() {
        return null;
    }
}

// FILE: JI2.java

public interface JI2 {
    String foo();
}

// FILE: JC2.java

public class JC2 implements JI2 {
    @Override
    public String foo() {
        return null;
    }
}

// FILE: KI.kt

interface KI {
    fun <T> foo(): T
}

// FILE: JKC.java

import java.util.List;

public class JKC implements KI {
    @Override
    public String foo() {
        return null;
    }
}

// FILE: test.kt

class C: JI2 by JC2(), <!IMPLEMENTATION_BY_DELEGATION_WITH_DIFFERENT_GENERIC_SIGNATURE_WARNING!>JI<!> by JC()

class C2: <!IMPLEMENTATION_BY_DELEGATION_WITH_DIFFERENT_GENERIC_SIGNATURE_WARNING!>KI<!> by JKC(), JI2 by JC2()

