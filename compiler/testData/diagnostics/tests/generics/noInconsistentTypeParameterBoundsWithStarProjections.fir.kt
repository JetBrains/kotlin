// ISSUE: KT-70667

interface RootType<T>

interface ChildType1<T> : RootType<T>

interface GrandChildType1<T> : ChildType1<T>

interface ChildType2<T> : RootType<T>

class Test<<!INCONSISTENT_TYPE_PARAMETER_BOUNDS!>T<!>> where T : ChildType2<*>, T : GrandChildType1<*>
