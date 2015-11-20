// See KT-9438: Enforce the Single Instantiation Inheritance Rule for type parameters

interface A

interface B

interface D<T>

interface CorrectF<<error descr="[INCONSISTENT_TYPE_PARAMETER_BOUNDS] Type parameter T of 'D' has inconsistent bounds: A, B">T</error>> where T : D<A>, T : <error descr="[REPEATED_BOUND] Type parameter already has this bound">D<B></error>

fun <<error descr="[INCONSISTENT_TYPE_PARAMETER_BOUNDS] Type parameter T of 'D' has inconsistent bounds: A, B">T</error>> bar() where T : D<A>, T : <error descr="[REPEATED_BOUND] Type parameter already has this bound">D<B></error> {}
