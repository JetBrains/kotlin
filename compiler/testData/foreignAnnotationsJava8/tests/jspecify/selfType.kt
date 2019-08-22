// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// FILE: A.java

import org.jspecify.annotations.*;

public class A<T extends @NotNull A<T>> {
    public void foo(T t) {}
}

// FILE: B.java
public class B extends A<B> {}

// FILE: C.java
import org.jspecify.annotations.*;
public class C<E extends @NotNull C<E>> extends A<E> {}

// FILE: main.kt

class AK : A<AK>()
class AKN : A<<!UPPER_BOUND_VIOLATED!>AK?<!>>()

class BK : B()

class CK : C<CK>()
class CKN : C<<!UPPER_BOUND_VIOLATED!>CK?<!>>()

fun main(
    ak: AK,
    akn: AKN,
    bk: BK,
    ck: CK,
    ckn: CKN
) {
    ak.foo(ak)
    ak.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)

    akn.foo(<!TYPE_MISMATCH!>akn<!>)
    akn.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)

    bk.foo(bk)
    bk.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)

    ck.foo(ck)
    ck.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)

    ckn.foo(<!TYPE_MISMATCH!>ckn<!>)
    ckn.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)
}
