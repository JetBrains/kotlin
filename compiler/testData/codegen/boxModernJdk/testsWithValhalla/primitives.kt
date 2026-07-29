// VALHALLA_SUPPORT: PRIMITIVES
// LANGUAGE: +FullValueClasses
// CHECK_BYTECODE_LISTING
// CHECK_BYTECODE_TEXT

value class Point(val x: Int, val y: Int)

@JvmInline
value class Id(val value: Int)

@JvmRecord
value class Record(val a: Int, val b: Int)

abstract value class AbstractValue

sealed value class SealedValue

value class SealedChild(val s: Int) : SealedValue()

fun box(): String {
    val point = Point(1, 2)
    if (point.x != 1 || point.y != 2) return "Point getters: $point"
    if (point != Point(1, 2) || point == Point(9, 9)) return "Point equals"
    if (point.toString() != "Point(x=1, y=2)") return "Point.toString: $point"

    val id = Id(42)
    if (id.value != 42) return "Id getter"
    if (id != Id(42) || id == Id(0)) return "Id equals"
    if (id.toString() != "Id(value=42)") return "Id.toString: $id"

    val record = Record(3, 4)
    if (record.a != 3 || record.b != 4) return "Record getters"
    if (record != Record(3, 4) || record == Record(0, 0)) return "Record equals"
    if (record.toString() != "Record(a=3, b=4)") return "Record.toString: $record"

    val sealedChild = SealedChild(6)
    if (sealedChild.s != 6) return "SealedChild getter"
    if (sealedChild !is SealedValue) return "SealedChild is not SealedValue"
    if (sealedChild.javaClass.superclass != SealedValue::class.java) return "SealedChild superclass: ${sealedChild.javaClass.superclass}"

    if (!SealedValue::class.java.isSealed) return "SealedValue is not sealed"
    val permitted = SealedValue::class.java.permittedSubclasses
    if (permitted == null || permitted.size != 1 || permitted[0] != SealedChild::class.java)
        return "SealedValue permittedSubclasses: ${permitted?.contentToString()}"

    return "OK"
}

// No value class is a Valhalla value class in this mode: every class keeps its identity (the ACC_SUPER / 0x20 access flag),
// so the concrete classes are 0x31 (0x10031 for the record), the abstract ones 0x421, and no instance field is strict.
// 1 access flags 0x31\npublic final class Point
// 1 access flags 0x31\npublic final class Id
// 1 access flags 0x10031\npublic final class Record
// 1 access flags 0x421\npublic abstract class AbstractValue
// 1 access flags 0x421\npublic abstract class SealedValue
// 1 access flags 0x31\npublic final class SealedChild
// 1 private final I x
// 1 private final I y
// 1 private final I value
// 1 private final I a
// 1 private final I b
// 1 private final I s
// 0 private final strictfp I
