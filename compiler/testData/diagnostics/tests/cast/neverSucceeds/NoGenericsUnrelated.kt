// !DIAGNOSTICS: -WARNING +CAST_NEVER_SUCCEEDS
interface Trait1
interface Trait2
open class OClass1
open class OClass2
class FClass1
class FClass2

fun <TP1: OClass1, TP2: OClass2> test(
  t1: Trait1,
  oc1: OClass1,
  fc1: FClass1,
  tp1: TP1
) {
  t1 as Trait2
  t1 as OClass2
  t1 <!CAST_NEVER_SUCCEEDS!>as<!> FClass2
  t1 as TP2

  oc1 as Trait2
  oc1 <!CAST_NEVER_SUCCEEDS!>as<!> OClass2
  oc1 <!CAST_NEVER_SUCCEEDS!>as<!> FClass2
  oc1 as TP2

  fc1 <!CAST_NEVER_SUCCEEDS!>as<!> Trait2
  fc1 <!CAST_NEVER_SUCCEEDS!>as<!> OClass2
  fc1 <!CAST_NEVER_SUCCEEDS!>as<!> FClass2
  fc1 as TP2

  tp1 as Trait2
  tp1 as OClass2
  tp1 as FClass2
  tp1 as TP2
}