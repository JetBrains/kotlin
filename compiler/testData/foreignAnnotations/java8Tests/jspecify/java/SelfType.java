import org.jspecify.annotations.*;

@DefaultNotNull
public class SelfType<T extends SelfType<T>> {
    public void foo(T t) {}
}

class B extends SelfType<B> {}

@DefaultNotNull
class C<E extends C<E>> extends SelfType<E> {}

class AK extends SelfType<AK> {}
class AKN extends SelfType<@Nullable AK> {}

class BK extends B {}

class CK extends C<CK> {}

class CKN extends C<@Nullable CK> {
    public void main(AK ak, AKN akn, BK bk, CK ck, CKN ckn) {
        ak.foo(ak);
        // jspecify_nullness_mismatch
        ak.foo(null);

        // jspecify_nullness_mismatch
        akn.foo(null);

        bk.foo(bk);
        // jspecify_nullness_mismatch
        bk.foo(null);

        ck.foo(ck);
        // jspecify_nullness_mismatch
        ck.foo(null);

        // jspecify_nullness_mismatch
        ckn.foo(null);
    }
}
