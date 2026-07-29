// VALHALLA_SUPPORT: ALL_VALUES
// LANGUAGE: +FullValueClasses
// CHECK_BYTECODE_TEXT

import java.time.LocalDate
import java.util.Optional

value class Val(val a: Int, val b: Int)

// A regular (identity) class holding value-based field types that must be listed: a user value class, a boxed primitive, and two
// non-primitive JDK value-based classes (`LocalDate`, `Optional`). A non-value-based JDK reference (`Number`), an identity
// reference (`String`) and a primitive (`int`) must NOT be listed — showing the whole value-based set is covered, not just wrappers.
class RegularHolder(val v: Val, val boxedInt: Int?, val date: LocalDate, val opt: Optional<String>, val num: Number?, val s: String, val prim: Int)

// A value class holding another value class and a boxed-primitive field.
value class ValueHolder(val v: Val, val boxed: Int?)

// A self-referential value class. Its own type must NOT be listed (the class is already being loaded), but the co-field of another
// value class (`Val`) must be — matching javac, which drops only the exact self-type.
value class SelfRef(val v: Val, val self: SelfRef?)

// Mutually-referential value classes: each must list the other (a cycle is not self-reference), plus their `Val` field. Matches javac.
value class Node1(val v: Val, val other: Node2?)
value class Node2(val v: Val, val other: Node1?)

// A static value-class field (top-level property), which javac also lists in `LoadableDescriptors`.
val staticVal: Val = Val(9, 9)

fun box(): String {
    val regular = RegularHolder(Val(1, 2), 3, LocalDate.of(2020, 1, 2), Optional.of("o"), 3.14, "s", 5)
    if (regular.v != Val(1, 2)) return "RegularHolder.v: ${regular.v}"
    if (regular.boxedInt != 3) return "RegularHolder.boxedInt: ${regular.boxedInt}"
    if (regular.date != LocalDate.of(2020, 1, 2)) return "RegularHolder.date: ${regular.date}"
    if (regular.opt.get() != "o") return "RegularHolder.opt: ${regular.opt}"
    if (regular.num != 3.14 || regular.s != "s" || regular.prim != 5) return "RegularHolder rest"

    val value = ValueHolder(Val(6, 7), 8)
    if (value.v != Val(6, 7) || value.boxed != 8) return "ValueHolder: $value"

    val selfRef = SelfRef(Val(1, 2), null)
    if (selfRef.v != Val(1, 2) || selfRef.self != null) return "SelfRef: $selfRef"

    val node1 = Node1(Val(3, 4), null)
    val node2 = Node2(Val(5, 6), null)
    if (node1.v != Val(3, 4) || node1.other != null) return "Node1: $node1"
    if (node2.v != Val(5, 6) || node2.other != null) return "Node2: $node2"

    if (staticVal != Val(9, 9)) return "staticVal: $staticVal"

    return "OK"
}

// The `LoadableDescriptors` attribute (JEP 401) lists the value-class field descriptors, matching javac. It is emitted on the
// regular class `RegularHolder`, the value classes `ValueHolder`/`SelfRef`/`Node1`/`Node2`, and the file class holding the static
// `staticVal` field — six attributes total. `Val` (only primitive fields) gets none. `RegularHolder` lists `Val`, the boxed
// `Integer`, and the JDK value-based `LocalDate` and `Optional`, but not `Number` (not value-based), `String` (identity) or the
// primitive `int`. `SelfRef` lists `Val` but NOT its own type `LSelfRef;`. `Node1`/`Node2` list `Val` and each other.
// 6 ATTRIBUTE LoadableDescriptors
// 1 ATTRIBUTE LoadableDescriptors : LVal;, Ljava/lang/Integer;, Ljava/time/LocalDate;, Ljava/util/Optional;\n
// 1 ATTRIBUTE LoadableDescriptors : LVal;, Ljava/lang/Integer;\n
// 2 ATTRIBUTE LoadableDescriptors : LVal;\n
// 1 ATTRIBUTE LoadableDescriptors : LVal;, LNode2;\n
// 1 ATTRIBUTE LoadableDescriptors : LVal;, LNode1;\n
