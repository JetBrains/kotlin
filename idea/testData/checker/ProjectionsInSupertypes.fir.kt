interface A<T> {}
interface B<T> {}
interface C<T> {}
interface D<T> {}

interface Test : A<in Int>, B<out Int>, C<*>??<error descr="[NULLABLE_SUPERTYPE] A supertype cannot be nullable">?</error>, D<Int> {}
