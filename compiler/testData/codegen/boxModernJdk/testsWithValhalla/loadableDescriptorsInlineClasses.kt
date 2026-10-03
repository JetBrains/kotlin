// VALHALLA_VALUE_CLASSES
// LANGUAGE: +FullValueClasses
// CHECK_BYTECODE_TEXT

// FILE: JavaVal.java
public value class JavaVal {
    public final int x;

    public JavaVal(int x) {
        this.x = x;
    }
}

// FILE: test.kt
import java.util.Optional

value class Val(val a: Int, val b: Int)

// Signatures use the underlying type of a non-null `@JvmInline` value.
@JvmInline
value class WrapVal(val v: Val)

@JvmInline
value class WrapJavaVal(val j: JavaVal)

@JvmInline
value class WrapBoxedInt(val i: Int?)

@JvmInline
value class WrapOptional(val o: Optional<String>)

@JvmInline
value class WrapWrapVal(val w: WrapVal)

@JvmInline
value class WrapString(val s: String)

@JvmInline
value class WrapInt(val i: Int)

class FieldHolder(
    val v: WrapVal, val j: WrapJavaVal, val b: WrapBoxedInt, val o: WrapOptional, val w: WrapWrapVal, val s: WrapString, val i: WrapInt,
)

class MethodHolder {
    fun take(v: WrapVal, i: WrapInt) {}
    fun give(): WrapJavaVal = WrapJavaVal(JavaVal(1))
}

// A type parameter bounded by a `@JvmInline` class is erased to the underlying type as well.
class GenericMethodHolder {
    fun <T : WrapVal> take(t: T): T = t
}

// The signature of a function exposed boxed uses the `@JvmInline` class itself.
@OptIn(ExperimentalStdlibApi::class)
@JvmExposeBoxed
fun exposed(v: WrapVal): WrapVal = v

fun box(): String {
    val holder = FieldHolder(
        WrapVal(Val(1, 2)), WrapJavaVal(JavaVal(3)), WrapBoxedInt(4), WrapOptional(Optional.of("o")), WrapWrapVal(WrapVal(Val(5, 6))),
        WrapString("s"), WrapInt(7),
    )
    if (holder.v.v != Val(1, 2)) return "FieldHolder.v: ${holder.v}"
    if (holder.j.j.x != 3) return "FieldHolder.j: ${holder.j.j.x}"
    if (holder.b.i != 4) return "FieldHolder.b: ${holder.b}"
    if (holder.o.o.get() != "o") return "FieldHolder.o: ${holder.o}"
    if (holder.w.w.v != Val(5, 6)) return "FieldHolder.w: ${holder.w}"
    if (holder.s.s != "s" || holder.i.i != 7) return "FieldHolder.s, i: ${holder.s}, ${holder.i}"
    val methods = MethodHolder()
    methods.take(WrapVal(Val(1, 1)), WrapInt(2))
    if (methods.give().j.x != 1) return "MethodHolder.give"
    if (GenericMethodHolder().take(WrapVal(Val(3, 3))).v != Val(3, 3)) return "GenericMethodHolder.take"
    if (exposed(WrapVal(Val(9, 9))).v != Val(9, 9)) return "exposed"
    return "OK"
}

// Like javac for fields and parameters of the underlying types, the classes above list the underlying value classes, but neither
// `String` nor `int`. The boxed signature of `exposed` lists `WrapVal` itself too.
// 9 ATTRIBUTE LoadableDescriptors
// 3 ATTRIBUTE LoadableDescriptors : LVal;\n
// 1 ATTRIBUTE LoadableDescriptors : LJavaVal;\n
// 1 ATTRIBUTE LoadableDescriptors : Ljava/lang/Integer;\n
// 1 ATTRIBUTE LoadableDescriptors : Ljava/util/Optional;\n
// 1 ATTRIBUTE LoadableDescriptors : LVal;, LJavaVal;, Ljava/lang/Integer;, Ljava/util/Optional;\n
// 1 ATTRIBUTE LoadableDescriptors : LVal;, LJavaVal;\n
// 1 ATTRIBUTE LoadableDescriptors : LVal;, LWrapVal;\n
