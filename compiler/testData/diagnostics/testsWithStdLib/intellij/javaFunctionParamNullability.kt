// RUN_PIPELINE_TILL: BACKEND
// JVM_TARGET: 1.8
// DUMP_INFERENCE_LOGS: FIXATION

// FILE: Function.java
public interface Function<I, O> {
    O fun(I param);
}

// FILE: Renderer.java
public abstract class Renderer<R> {
    public static <S> Renderer<S> create(Function <? super S, String> getText) {
        return null;
    }
}

// FILE: test.kt

interface In<in X>

fun <E> mtIn(): In<E> = TODO()

fun <T> comboBox1(renderer: Renderer<in T?>): T = TODO()
fun <T> comboBox2(renderer: Renderer<in T?>, w: In<T>): T = TODO()

fun test() {
    comboBox1<String>(
        Renderer.create { // it should be flexible
            it.substring(1) // OK
        }
    )

    comboBox2<String>(
        Renderer.create {
            it.substring(1) // Unsafe call
        },
        w = mtIn(),
    )
}

/* GENERATED_FIR_TAGS: elvisExpression, equalityExpression, flexibleType, functionDeclaration, inProjection,
integerLiteral, javaFunction, javaProperty, javaType, lambdaLiteral, nullableType, safeCall, samConversion,
stringLiteral, typeParameter, whenExpression, whenWithSubject */
