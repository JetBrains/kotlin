// FILE: JFooWithUpperBound.java
public interface JFooWithUpperBound<T extends IBase> {
    T foo();
}

// FILE: JFooWithUpperBoundDerived.java
public interface JFooWithUpperBoundDerived<T extends IBase> extends JFooWithUpperBound<T> {
}

// FILE: JCFooWithUpperBound.java
public class JCFooWithUpperBound<T extends IBase> {
    public T foo() {
        return null;
    }
}

// FILE: JCFooWithUpperBoundDerived.java
public class JCFooWithUpperBoundDerived<T extends IBase> extends JCFooWithUpperBound<T> {
}

// FILE: K.kt
interface IBase

interface IDerived : IBase

interface IFooWithUpperBound<T : IBase> {
    fun foo(): T
}

interface IFooT<T> {
    fun foo(): T
}

interface IFoo {
    fun foo(): IBase
}

interface IFooDerived : IFoo {
    override fun foo(): IDerived
}

interface IFooWithUpperBoundDerived<T : IBase> : IFooWithUpperBound<T>

interface Test1<T : IBase> : IFooWithUpperBound<T>, IFoo

interface Test2<T : IBase> : IFooT<T>, IFoo

interface Test3<T : IDerived> : IFooWithUpperBoundDerived<T>, IFooDerived

interface Test4<T : IBase> : JFooWithUpperBound<T>, IFoo

interface Test5<T : IDerived> : JFooWithUpperBoundDerived<T>, IFooDerived

class Test6<T : IBase> : JCFooWithUpperBound<T>(), IFoo

class Test7<T : IDerived> : JCFooWithUpperBoundDerived<T>(), IFooDerived