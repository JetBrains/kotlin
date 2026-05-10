// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters +InferThrowableTypeParameterToUpperBound
// ISSUE: KT-82961

// FILE: ThrowableCallable.java
public interface ThrowableCallable<E extends Throwable> {
    void call() throws E;
}

// FILE: KotlinInterface.kt
interface KotlinInterface {
    fun <E1 : Throwable> basic(t: ThrowableCallable<E1>)

    context(c: List<E2>?)
    fun <E2 : Throwable> withContext(t: ThrowableCallable<E2>)

    fun <E3 : Throwable> List<E3>?.withReceiver(t: ThrowableCallable<E3>)
}

// FILE: JavaDerived.java
import java.util.List;

public class JavaDerived implements KotlinInterface {
    @Override
    public <E1 extends Throwable> void basic(ThrowableCallable<E1> t) {}

    @Override
    public <E2 extends Throwable> void withContext(List<? extends E2> c, ThrowableCallable<E2> t) {}

    @Override
    public <E3 extends Throwable> void withReceiver(List<? extends E3> p, ThrowableCallable<E3> t) {}
}

// FILE: test.kt
fun Nothing?.test(j: JavaDerived) {
    j.basic {}
    j.<!CANNOT_INFER_PARAMETER_TYPE!>withContext<!> {}
}

fun JavaDerived.test(l: Nothing?) {
    l.<!CANNOT_INFER_PARAMETER_TYPE!>withReceiver<!> {}
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, javaType, lambdaLiteral, samConversion */
