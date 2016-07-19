// KT-702 Type inference failed
fun <T> getJavaClass() : java.lang.Class<T> { return "" <!CAST_NEVER_SUCCEEDS!>as<!> Class<T> }

public class Throwables() {
    companion object {
        public fun <X : Throwable?> propagateIfInstanceOf(throwable : Throwable?, declaredType : Class<X?>?) {
            if (((throwable != null) && declaredType?.isInstance(throwable)!!))
            {
                throw declaredType<!UNNECESSARY_SAFE_CALL!>?.<!>cast(throwable)!!
            }
        }
        public fun propagateIfPossible(throwable : Throwable?) {
            propagateIfInstanceOf(throwable, getJavaClass<Error?>()) //; Type inference failed: Mismatch while expanding constraints
            propagateIfInstanceOf(throwable, getJavaClass<RuntimeException?>()) // Type inference failed: Mismatch while expanding constraints
        }
    }
}