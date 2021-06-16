interface A<T> {}
interface B<T> {}
interface C<T> {}
interface D<T> {}

interface Test : A<<error descr="[PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE] Projections are not allowed for immediate arguments of a supertype">in</error> Int>, B<<error descr="[PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE] Projections are not allowed for immediate arguments of a supertype">out</error> Int>, C<<error descr="[PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE] Projections are not allowed for immediate arguments of a supertype">*</error>>??<error descr="[NULLABLE_SUPERTYPE] A supertype cannot be nullable">?</error>, D<Int> {}
