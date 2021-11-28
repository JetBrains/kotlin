interface A
interface B : A

interface ListA : List<A>
interface ListB : List<B>

interface Z<<!INCONSISTENT_TYPE_PARAMETER_BOUNDS!>T<!>> where T : ListA, T : ListB
