interface A<T> {}
interface B<T> {}
interface C<T> {}
interface D<T> {}

interface Test : A<in Int>, B<out Int>, C<*>??<!NULLABLE_SUPERTYPE!>?<!>, D<Int> {}
