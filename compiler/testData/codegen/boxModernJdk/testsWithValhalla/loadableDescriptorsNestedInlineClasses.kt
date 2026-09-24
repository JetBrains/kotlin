// VALHALLA_VALUE_CLASSES
// LANGUAGE: +FullValueClasses
// CHECK_BYTECODE_TEXT

// FILE: inlineClasses.kt
value class Val(val a: Int, val b: Int)

@JvmInline
value class WrapVal(val v: Val)

@JvmInline
value class WrapWrapVal(val w: WrapVal)

@JvmInline
value class WrapInt(val i: Int)

@JvmInline
value class WrapWrapInt(val w: WrapInt)

@JvmInline
value class Bounded<T : WrapWrapVal>(val t: T)

@JvmInline
value class Unbounded<T>(val t: T)

// FILE: holders.kt
// Each class uses one kind of type, either in a private field only or in method signatures only. A nested `@JvmInline` class is
// represented by its innermost underlying type, and a type parameter by its erasure: the bound, or `Object` without one.
class NestedField {
    private val w = WrapWrapVal(WrapVal(Val(1, 2)))
}

class NestedSignature {
    fun take(w: WrapWrapVal): WrapWrapVal = w
}

class NullableNestedSignature {
    fun take(w: WrapWrapVal?): WrapWrapVal? = w
}

class NestedIntField {
    private val w = WrapWrapInt(WrapInt(3))
}

class NullableNestedIntField {
    private val w: WrapWrapInt? = WrapWrapInt(WrapInt(4))
}

class NestedIntSignature {
    fun take(w: WrapWrapInt): WrapWrapInt = w
}

class NullableNestedIntSignature {
    fun take(w: WrapWrapInt?): WrapWrapInt? = w
}

class BoundedField {
    private val b = Bounded(WrapWrapVal(WrapVal(Val(5, 6))))
}

class BoundedSignature {
    fun take(b: Bounded<WrapWrapVal>): Bounded<WrapWrapVal> = b
}

class UnboundedField {
    private val u = Unbounded(WrapWrapVal(WrapVal(Val(7, 8))))
}

class UnboundedSignature {
    fun take(u: Unbounded<WrapWrapVal>): Unbounded<WrapWrapVal> = u
}

class TypeParameterField<T : WrapWrapVal> {
    private val t: T? = null
}

class TypeParameterSignature {
    fun <T : WrapWrapVal> take(t: T): T = t
}

class NullableTypeParameterSignature {
    fun <T : WrapWrapVal?> take(t: T): T = t
}

class UnboundedTypeParameterSignature {
    fun <T> take(t: T): T = t
}

fun box(): String {
    val w = WrapWrapVal(WrapVal(Val(1, 2)))
    val i = WrapWrapInt(WrapInt(3))
    NestedField()
    if (NestedSignature().take(w) != w) return "NestedSignature"
    if (NullableNestedSignature().take(w) != w) return "NullableNestedSignature"
    NestedIntField()
    NullableNestedIntField()
    if (NestedIntSignature().take(i) != i) return "NestedIntSignature"
    if (NullableNestedIntSignature().take(i) != i) return "NullableNestedIntSignature"
    BoundedField()
    if (BoundedSignature().take(Bounded(w)).t != w) return "BoundedSignature"
    UnboundedField()
    if (UnboundedSignature().take(Unbounded(w)).t != w) return "UnboundedSignature"
    TypeParameterField<WrapWrapVal>()
    if (TypeParameterSignature().take(w) != w) return "TypeParameterSignature"
    if (NullableTypeParameterSignature().take(w) != w) return "NullableTypeParameterSignature"
    if (UnboundedTypeParameterSignature().take(w) != w) return "UnboundedTypeParameterSignature"
    return "OK"
}

// @WrapWrapVal.class:
// 1 ATTRIBUTE LoadableDescriptors : LVal;\n
// @WrapWrapInt.class:
// 1 ATTRIBUTE LoadableDescriptors : I\n
// @Bounded.class:
// 0 ATTRIBUTE LoadableDescriptors
// @Unbounded.class:
// 0 ATTRIBUTE LoadableDescriptors
// @NestedField.class:
// 1 ATTRIBUTE LoadableDescriptors : LVal;\n
// @NestedSignature.class:
// 0 ATTRIBUTE LoadableDescriptors
// @NullableNestedSignature.class:
// 0 ATTRIBUTE LoadableDescriptors
// @NestedIntField.class:
// 1 ATTRIBUTE LoadableDescriptors : I\n
// @NullableNestedIntField.class:
// 1 ATTRIBUTE LoadableDescriptors : LWrapWrapInt;\n
// @NestedIntSignature.class:
// 0 ATTRIBUTE LoadableDescriptors
// @NullableNestedIntSignature.class:
// 0 ATTRIBUTE LoadableDescriptors
// @BoundedField.class:
// 1 ATTRIBUTE LoadableDescriptors : LVal;\n
// @BoundedSignature.class:
// 0 ATTRIBUTE LoadableDescriptors
// @UnboundedField.class:
// 1 ATTRIBUTE LoadableDescriptors : Ljava/lang/Object;\n
// @UnboundedSignature.class:
// 0 ATTRIBUTE LoadableDescriptors
// @TypeParameterField.class:
// 0 ATTRIBUTE LoadableDescriptors
// @TypeParameterSignature.class:
// 0 ATTRIBUTE LoadableDescriptors
// @NullableTypeParameterSignature.class:
// 0 ATTRIBUTE LoadableDescriptors
// @UnboundedTypeParameterSignature.class:
// 0 ATTRIBUTE LoadableDescriptors
