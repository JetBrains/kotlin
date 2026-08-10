// VALHALLA_SUPPORT: PRIMITIVES
// LANGUAGE: +FullValueClasses
// CHECK_BYTECODE_TEXT

value class FullVal(val x: Int)

@JvmInline
value class InlineVal(val y: Int)

// The inline field is nullable so it stays boxed as `LInlineVal;` (a non-null inline field would be unboxed to `I` and never listed).
class Holder(val full: FullVal, val inline: InlineVal?, val boxed: Int?)

fun box(): String {
    val holder = Holder(FullVal(1), InlineVal(2), 3)
    if (holder.full != FullVal(1)) return "Holder.full: ${holder.full}"
    if (holder.inline != InlineVal(2)) return "Holder.inline: ${holder.inline}"
    if (holder.boxed != 3) return "Holder.boxed: ${holder.boxed}"
    return "OK"
}

// 1 ATTRIBUTE LoadableDescriptors
// 1 ATTRIBUTE LoadableDescriptors : Ljava/lang/Integer;\n
