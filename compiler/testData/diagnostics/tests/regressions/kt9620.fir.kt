// KT-9620 AssertionError in checkBounds

interface E1<T : <!TYPE_ARGUMENTS_NOT_ALLOWED!>D<String><!>, D>

interface A
interface B
interface D<X>
interface E2<T : <!TYPE_ARGUMENTS_NOT_ALLOWED!>D<A><!>, D<!SYNTAX!><<!><!SYNTAX!>B<!><!SYNTAX!>><!><!SYNTAX!>><!>

// KT-11354 AE from DescriptorResolver

open class L<E>()

class M<C> : L<<!TYPE_ARGUMENTS_NOT_ALLOWED!>C<C><!>>()
