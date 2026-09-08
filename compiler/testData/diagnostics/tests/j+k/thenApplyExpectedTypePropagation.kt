// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-88593

// FILE: F.java
public interface F<T, R> {
    R apply(T t);
}

// FILE: Promise.java
public interface Promise<T> {
    <U> Promise<U> thenApply(F<? super T, ? extends U> fn);
}

// FILE: Pair.java
public class Pair<A, B> {
    public static <A, B> Pair<A, B> create(A first, B second) {
        return null;
    }
}

// FILE: Handle.java
public interface Handle<N, E> {
}

// FILE: Editor.java
public interface Editor {
}

// FILE: DiagramEditor.java
public interface DiagramEditor extends Editor {
}

// FILE: Builder.java
import org.jetbrains.annotations.Nullable;

public class Builder {
    public @Nullable DiagramEditor getEditor() {
        return null;
    }
}

// FILE: main.kt
class HandleImpl<N : Any, E : Any> : Handle<N, E> {
    lateinit var builder: Builder
}

interface Factory {
    fun <N : Any, E : Any> show(): Promise<Pair<Handle<N, E>, Editor>>
}

class FactoryImpl : Factory {
    override fun <N : Any, E : Any> show(): Promise<Pair<Handle<N, E>, Editor>> {
        return helper<N, E>().thenApply { Pair.create(it, it.builder.editor) }
    }

    fun <N : Any, E : Any> helper(): Promise<HandleImpl<N, E>> = null!!
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, flexibleType, functionDeclaration, inProjection,
interfaceDeclaration, javaFunction, javaProperty, javaType, lambdaLiteral, lateinit, nullableType, outProjection,
override, propertyDeclaration, samConversion, typeConstraint, typeParameter */
