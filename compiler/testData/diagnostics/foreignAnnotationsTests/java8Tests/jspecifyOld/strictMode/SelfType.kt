// FIR_IDENTICAL
// JSPECIFY_STATE: strict

// FILE: SelfType.java
import org.jspecify.nullness.*;

@NullMarked
public class SelfType<T extends SelfType<T>> {
    public void foo(T t) {}
}

// FILE: B.java
public class B extends SelfType<B> {}

// FILE: C.java
import org.jspecify.nullness.*;

@NullMarked
public class C<E extends C<E>> extends SelfType<E> {}

// FILE: AK.java
public class AK extends SelfType<AK> {}

// FILE: AKN.java
import org.jspecify.nullness.*;

public class AKN extends SelfType<@Nullable AK> {}

// FILE: BK.java
public class BK extends B {}

// FILE: CK.java
public class CK extends C<CK> {}

// FILE: CKN.java
import org.jspecify.nullness.*;

public class CKN extends C<@Nullable CK> {}

// FILE: main.kt
fun main(ak: AK, akn: AKN, bk: BK, ck: CK, ckn: CKN): Unit {
    ak.foo(ak)
    // jspecify_nullness_mismatch
    ak.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)

    // jspecify_nullness_mismatch
    akn.foo(null)

    bk.foo(bk)
    // jspecify_nullness_mismatch
    bk.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)

    ck.foo(ck)
    // jspecify_nullness_mismatch
    ck.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)

    // jspecify_nullness_mismatch
    ckn.foo(null)
}
