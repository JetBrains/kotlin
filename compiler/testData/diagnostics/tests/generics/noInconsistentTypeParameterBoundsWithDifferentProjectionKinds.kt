// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL

interface RootType<T>

interface ChildType1<T> : RootType<T>

interface GrandChildType1<T> : ChildType1<T>

interface ChildType2<T> : RootType<T>

class Test<T> where T : ChildType2<out T>, T : GrandChildType1<in T>