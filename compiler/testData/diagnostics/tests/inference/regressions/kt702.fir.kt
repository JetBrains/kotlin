//KT-702 Type inference failed
package a
//+JDK

fun <T> getJavaClass() : java.lang.Class<T> { }

public class Throwables() {
    companion object {
        public fun <X : Throwable?> propagateIfInstanceOf(throwable : Throwable?, declaredType : Class<X?>?) : Unit {
            if (((throwable != null) && declaredType?.isInstance(throwable)!!))
            {
                throw declaredType?.cast(throwable)!!
            }
        }
        public fun propagateIfPossible(throwable : Throwable?) : Unit {
            <!INAPPLICABLE_CANDIDATE!>propagateIfInstanceOf<!>(throwable, getJavaClass<Error?>()) // Type inference failed: Mismatch while expanding constraints
            <!INAPPLICABLE_CANDIDATE!>propagateIfInstanceOf<!>(throwable, getJavaClass<RuntimeException?>()) // Type inference failed: Mismatch while expanding constraints
        }
    }
}