private open class A

internal open class B

public open class C {
    protected open class D
}

public open class E

// invalid, A is private
public class Test1<T: <!EXPOSED_TYPE_PARAMETER_BOUND!>A<!>>

// valid, both type parameters is public
public class Test2<T: C, P: E>

// invalid, D is protected
public class Test3<T: E, P: <!EXPOSED_TYPE_PARAMETER_BOUND!>C.<!INVISIBLE_REFERENCE!>D<!><!>>

// valid, B is internal
internal class Test4<T: B>

// valid, B is internal
private class Test5<T: B>

public class Container : <!SUPERTYPE_NOT_INITIALIZED!>C<!> {
    // valid, D is protected in C
    protected class Test6<T: C.D>

    // invalid, B is internal
    protected class Test7<T: <!EXPOSED_TYPE_PARAMETER_BOUND!>B<!>>
}

// invalid, A is private, B is internal, D is protected
public interface Test8<T: <!EXPOSED_TYPE_PARAMETER_BOUND!>A<!>, P: <!EXPOSED_TYPE_PARAMETER_BOUND!>B<!>, F: C, N: <!EXPOSED_TYPE_PARAMETER_BOUND!>C.<!INVISIBLE_REFERENCE!>D<!><!>, M: E>
