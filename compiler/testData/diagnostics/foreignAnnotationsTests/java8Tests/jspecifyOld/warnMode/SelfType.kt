// JSPECIFY_STATE: warn
// MUTE_FOR_PSI_CLASS_FILES_READING

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
    ak.foo(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)

    akn.foo(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>) // the corresponding warning/error is present on the Java side

    bk.foo(bk)
    // jspecify_nullness_mismatch
    bk.foo(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)

    ck.foo(ck)
    // jspecify_nullness_mismatch
    ck.foo(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)

    ckn.foo(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>) // the corresponding warning/error is present on the Java side
}
