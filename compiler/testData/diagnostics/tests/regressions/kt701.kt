// KT-702 Type inference failed
fun getJavaClass<T>() : java.lang.Class<T> { return "" <!CAST_NEVER_SUCCEEDS!>as<!> Class<T> }

public class Throwables() {
    class object {
        public fun propagateIfInstanceOf<X : Throwable?>(throwable : Throwable?, declaredType : Class<X?>?) {
            if (((throwable != null) && declaredType?.isInstance(throwable).sure()))
            {
                throw declaredType?.cast(throwable).sure()
            }
        }
        public fun propagateIfPossible(throwable : Throwable?) {
            propagateIfInstanceOf(throwable, getJavaClass<Error?>()) //; Type inference failed: Mismatch while expanding constraints
            propagateIfInstanceOf(throwable, getJavaClass<RuntimeException?>()) // Type inference failed: Mismatch while expanding constraints
        }
    }
}
