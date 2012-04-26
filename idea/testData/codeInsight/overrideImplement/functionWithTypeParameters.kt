trait Trait {
    fun <A, B : Runnable, E : java.util.Map.Entry<A, B>> foo() where B : Cloneable, B : Comparable<B>;
}

class TraitImpl : Trait {
    <caret>
}