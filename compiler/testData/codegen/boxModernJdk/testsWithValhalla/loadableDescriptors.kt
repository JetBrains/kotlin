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

    if (staticVal != Val(9, 9)) return "staticVal: $staticVal"

    return "OK"
}

// The `LoadableDescriptors` attribute (JEP 401) lists the value-class field descriptors, matching javac. It is emitted on the
// regular class `RegularHolder`, the value class `ValueHolder`, and the file class holding the static `staticVal` field — three
// attributes total. `Val` (only primitive fields) gets none. `RegularHolder` lists `Val`, the boxed `Integer`, and the JDK
// value-based `LocalDate` and `Optional`, but not `Number` (not value-based), `String` (identity) or the primitive `int`.
// 3 ATTRIBUTE LoadableDescriptors
// 1 ATTRIBUTE LoadableDescriptors : LVal;, Ljava/lang/Integer;, Ljava/time/LocalDate;, Ljava/util/Optional;\n
// 1 ATTRIBUTE LoadableDescriptors : LVal;, Ljava/lang/Integer;\n
// 1 ATTRIBUTE LoadableDescriptors : LVal;\n
