// ISSUE: KT-62134

// FILE: Base.java

public class Base<T> {
    public T foo() {
        return null;
    }
}

// FILE: test.kt

class Bar

class Foo<in T> : Base<<!TYPE_VARIANCE_CONFLICT_ERROR!>T<!>>() {

    private val dnn: T & Any = TODO()

    private val flex = foo()

    fun bar(f: Foo<Bar>) {
        val dnn = f.<!INVISIBLE_MEMBER!>dnn<!>
        val flex = f.<!INVISIBLE_MEMBER!>flex<!>
    }
}
