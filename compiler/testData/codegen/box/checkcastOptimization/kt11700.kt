// TARGET_BACKEND: JVM_IR

// FILE: j/JBase.java
package j;

interface JBase {
    void foo();
}

// FILE: j/JSub.java
package j;
public interface JSub extends JBase{}

// FILE: j/JBaseImpl.java
package j;
public class JBaseImpl implements JBase {
    @Override
    public void foo() {}
}

// FILE: j/KBaseImpl.kt
package j;
public class KBaseImpl : JBase {
    override fun foo() {}
}

// FILE: j/JSubImpl.java
package j;
public class JSubImpl implements JSub {
    @Override
    public void foo() {}
}

// FILE: j/JOperation.java
package j;
public class JOperation {
    public void forJBase(JBase jb) {
        jb.foo();
    }
    public <T extends JBase> void forBoundedGenerics(T t) {
        t.foo();
    }
    public <T extends Object & JBase> void fortMultipleBounds(T t) {
        t.foo();
    }
}

// FILE: main.kt
package k

import j.JBaseImpl
import j.JSubImpl
import j.KBaseImpl
import j.JOperation
import j.JSub

fun test() {
    JOperation().forJBase(JBaseImpl()) // do not generate redundant CHECKCAST to j/JBase
    JOperation().forJBase(KBaseImpl()) // do not generate redundant CHECKCAST to j/JBase
    JOperation().forBoundedGenerics(JBaseImpl()) // do not generate redundant CHECKCAST to j/JBase
    JOperation().fortMultipleBounds(JBaseImpl()) // do not generate redundant CHECKCAST to java/lang/Object
}

fun <T : JBaseImpl> testBoundedGenericsParamTypeOfCaller(t: T) {
    JOperation().forJBase(t) // do not generate redundant CHECKCAST to j/JBase
}

fun <T> testMultipleBoundsParamTypeOfCaller(t: T) where T : Any, T : JSub {
    JOperation().forJBase(t) // CHECKCAST to j/Jub
}

fun box(): String {
    test()
    testBoundedGenericsParamTypeOfCaller(JBaseImpl())
    testMultipleBoundsParamTypeOfCaller(JSubImpl())
    return "OK"
}