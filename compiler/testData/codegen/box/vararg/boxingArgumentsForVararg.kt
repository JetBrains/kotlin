fun <T> anyVararg(vararg x: T): Boolean = x[0] == 123f

fun boxingNullablePrimitiveToAny(x: Float?): Boolean {
    if (x !== null) {
        return anyVararg(x)
    }
    return false
}

fun boxingPrimitiveToAny(x: Float): Boolean =
    anyVararg(x)

fun primitiveVararg(vararg x: Float): Boolean = x[0] == 123f

fun unboxingNullablePrimitiveToPrimitive(x: Float?): Boolean {
    if (x !== null) {
        return primitiveVararg(x)
    }
    return false
}

fun noBoxingPrimitiveToPrimitive(x: Float): Boolean =
    primitiveVararg(x)

inline class InlineClass(val x: Float)

fun <T : InlineClass> valueClassAnyVararg(vararg x: T): Boolean = x[0].x == 123f

fun boxingInlineClassToAny(x: InlineClass): Boolean =
    valueClassAnyVararg(x)


fun box(): String {
    if (!boxingNullablePrimitiveToAny(123f)) return "Fail1"
    if (!boxingPrimitiveToAny(123f)) return "Fail2"
    if (!unboxingNullablePrimitiveToPrimitive(123f)) return "Fail3"
    if (!noBoxingPrimitiveToPrimitive(123f)) return "Fail4"
    if (!boxingInlineClassToAny(InlineClass(123f))) return "Fail5"
    return "OK"
}