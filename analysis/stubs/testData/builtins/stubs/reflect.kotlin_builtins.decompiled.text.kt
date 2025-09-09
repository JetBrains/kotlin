// IntelliJ API Decompiler stub source generated from a class file
// Implementation of methods is not available

package kotlin.reflect

public expect interface KCallable<out R> {
    @kotlin.internal.IntrinsicConstEvaluation public expect abstract val name: kotlin.String
}

public expect interface KClass<T : kotlin.Any> : kotlin.reflect.KClassifier {
    public expect abstract val simpleName: kotlin.String?

    public expect abstract val qualifiedName: kotlin.String?

    @kotlin.SinceKotlin public expect abstract fun isInstance(value: kotlin.Any?): kotlin.Boolean

    public expect abstract operator fun equals(other: kotlin.Any?): kotlin.Boolean

    public expect abstract fun hashCode(): kotlin.Int
}

@kotlin.SinceKotlin public interface KClassifier {
}

public expect interface KFunction<out R> : kotlin.reflect.KCallable<R>, kotlin.Function<R> {
}

public expect interface KMutableProperty<V> : kotlin.reflect.KProperty<V> {
}

public expect interface KMutableProperty0<V> : kotlin.reflect.KProperty0<V>, kotlin.reflect.KMutableProperty<V> {
    public expect abstract fun set(value: V): kotlin.Unit
}

public expect interface KMutableProperty1<T, V> : kotlin.reflect.KProperty1<T, V>, kotlin.reflect.KMutableProperty<V> {
    public expect abstract fun set(receiver: T, value: V): kotlin.Unit
}

public expect interface KMutableProperty2<D, E, V> : kotlin.reflect.KProperty2<D, E, V>, kotlin.reflect.KMutableProperty<V> {
    public expect abstract fun set(receiver1: D, receiver2: E, value: V): kotlin.Unit
}

public expect interface KProperty<out V> : kotlin.reflect.KCallable<V> {
}

public expect interface KProperty0<out V> : kotlin.reflect.KProperty<V>, () -> V {
    public expect abstract fun get(): V
}

public expect interface KProperty1<T, out V> : kotlin.reflect.KProperty<V>, (T) -> V {
    public expect abstract fun get(receiver: T): V
}

public expect interface KProperty2<D, E, out V> : kotlin.reflect.KProperty<V>, (D, E) -> V {
    public expect abstract fun get(receiver1: D, receiver2: E): V
}

public expect interface KType {
    @kotlin.SinceKotlin public expect abstract val classifier: kotlin.reflect.KClassifier?

    @kotlin.SinceKotlin public expect abstract val arguments: kotlin.collections.List<kotlin.reflect.KTypeProjection>

    public expect abstract val isMarkedNullable: kotlin.Boolean
}

@kotlin.SinceKotlin public interface KTypeParameter : kotlin.reflect.KClassifier {
    public abstract val name: kotlin.String

    public abstract val upperBounds: kotlin.collections.List<kotlin.reflect.KType>

    public abstract val variance: kotlin.reflect.KVariance

    public abstract val isReified: kotlin.Boolean
}

@kotlin.SinceKotlin public final data class KTypeProjection public constructor(variance: kotlin.reflect.KVariance?, type: kotlin.reflect.KType?) {
    public companion object {
        @kotlin.PublishedApi internal final val star: kotlin.reflect.KTypeProjection /* compiled code */

        public final val STAR: kotlin.reflect.KTypeProjection /* compiled code */
            public final get() { /* compiled code */ }

        @kotlin.jvm.JvmStatic public final fun invariant(type: kotlin.reflect.KType): kotlin.reflect.KTypeProjection { /* compiled code */ }

        @kotlin.jvm.JvmStatic public final fun contravariant(type: kotlin.reflect.KType): kotlin.reflect.KTypeProjection { /* compiled code */ }

        @kotlin.jvm.JvmStatic public final fun covariant(type: kotlin.reflect.KType): kotlin.reflect.KTypeProjection { /* compiled code */ }
    }

    public final val variance: kotlin.reflect.KVariance? /* compiled code */

    public final val type: kotlin.reflect.KType? /* compiled code */

    public open fun toString(): kotlin.String { /* compiled code */ }

    public final operator fun component1(): kotlin.reflect.KVariance? { /* compiled code */ }

    public final operator fun component2(): kotlin.reflect.KType? { /* compiled code */ }

    public final fun copy(variance: kotlin.reflect.KVariance? = COMPILED_CODE, type: kotlin.reflect.KType? = COMPILED_CODE): kotlin.reflect.KTypeProjection { /* compiled code */ }

    public open operator fun equals(other: kotlin.Any?): kotlin.Boolean { /* compiled code */ }

    public open fun hashCode(): kotlin.Int { /* compiled code */ }
}

@kotlin.SinceKotlin public final enum class KVariance private constructor() : kotlin.Enum<kotlin.reflect.KVariance> {
    INVARIANT,

    IN,

    OUT;
}
