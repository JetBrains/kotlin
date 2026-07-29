// VALHALLA_SUPPORT: PRIMITIVES
// LANGUAGE: +FullValueClasses
// CHECK_BYTECODE_LISTING
// CHECK_JVM_FLAGS

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

// No value class is a Valhalla value class in this mode: every class keeps its identity (the ACC_SUPER access flag is present),
// and no instance field is strict (no ACC_STRICT).

// TESTED_OBJECT_KIND: class
// TESTED_OBJECTS: Point
// FLAGS: ACC_PUBLIC, ACC_FINAL, ACC_SUPER

// TESTED_OBJECT_KIND: class
// TESTED_OBJECTS: Id
// FLAGS: ACC_PUBLIC, ACC_FINAL, ACC_SUPER

// TESTED_OBJECT_KIND: class
// TESTED_OBJECTS: Record
// FLAGS: ACC_PUBLIC, ACC_FINAL, ACC_SUPER, ACC_RECORD

// TESTED_OBJECT_KIND: class
// TESTED_OBJECTS: AbstractValue
// FLAGS: ACC_PUBLIC, ACC_SUPER, ACC_ABSTRACT

// TESTED_OBJECT_KIND: class
// TESTED_OBJECTS: SealedValue
// FLAGS: ACC_PUBLIC, ACC_SUPER, ACC_ABSTRACT

// TESTED_OBJECT_KIND: class
// TESTED_OBJECTS: SealedChild
// FLAGS: ACC_PUBLIC, ACC_FINAL, ACC_SUPER

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Point, x
// FLAGS: ACC_PRIVATE, ACC_FINAL

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Point, y
// FLAGS: ACC_PRIVATE, ACC_FINAL

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Id, value
// FLAGS: ACC_PRIVATE, ACC_FINAL

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Record, a
// FLAGS: ACC_PRIVATE, ACC_FINAL

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Record, b
// FLAGS: ACC_PRIVATE, ACC_FINAL

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: SealedChild, s
// FLAGS: ACC_PRIVATE, ACC_FINAL
