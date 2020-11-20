// FIR_IDENTICAL
// !DIAGNOSTICS: -WARNING +CAST_NEVER_SUCCEEDS
interface T1
interface T2
interface T3
open class OC1: T1
open class OC2: OC1(), T2
class FC1: OC2(), T3
interface T4: <!INTERFACE_WITH_SUPERCLASS!>OC1<!>
interface T5: T2

fun <TP1: OC1, TP2: T2, TP3: OC2> test(
    t2: T2,
    t4: T4,
    fc1: FC1,
    oc1: OC1,
    oc2: OC2,
    tp1: TP1,
    tp2: TP2
) {
  fc1 as FC1
  fc1 as OC1
  fc1 as T1
  fc1 as TP1

  oc1 as FC1
  oc1 as OC2
  oc2 as OC1
  oc1 as T2
  oc1 as T1
  oc1 as TP1
  oc1 as TP2

  t2 as FC1
  t2 as OC2
  t4 as OC1
  t2 as T2
  t2 as T5
  t2 as TP2

  tp1 as FC1
  tp1 as OC1
  tp1 as OC2
  tp2 as T2
  tp2 as T5
  tp1 as TP3
}