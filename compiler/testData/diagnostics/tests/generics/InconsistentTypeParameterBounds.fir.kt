interface A
interface B : A

interface ListA : List<A>
interface ListB : List<B>

interface Z<T> where T : ListA, T : ListB
