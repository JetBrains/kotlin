package testData

class Pair<out A, out B> (val first: A, val second: B}

open class Base_T<T>()
open class Derived_T<T>() : Base_T<T>
open class DDerived_T<T>() : Derived_T<T>
open class DDerived1_T<T>() : Derived_T<T>
open class DDerived2_T<T>() : Derived_T<T>, Base_T<T>
open class Base_inT<in T>()
open class Derived_inT<in T>() : Base_inT<T>
open class Derived1_inT<in T>() : Base_inT<T>, Derived_T<T>
open class Base_outT<out T>()
open class Derived_outT<out T>() : Base_outT<T>
open class MDerived_T<T>() : Base_outT<out T>, Base_T<T>

class Properties() { val p : Int }
class Props<T>() { val p : T }
class Functions<T>() {
  fun f() : Unit {}
  fun f(a : Int) : Int {}
  fun f(a : T) : Any {}
  fun f(a : Pair<Int, Int>) : T {}
  fun f<E>(a : E) : T {}
}
class WithPredicate() {
  fun isValid() : Boolean
  fun isValid(x : Int) : Boolean
  val p : Boolean
}

open class InvList<E>()
open class AbstractList<E> : InvList<E?>
open class ArrayList<E>() : Any, AbstractList<E?>, InvList<E?>

fun f() : Unit {}
fun f(a : Int) : Int {a}
fun f(a : Float, b : Int) : Float {a}
fun f<T>(a : Float) : T {a}

trait Parent
trait A: Parent
trait B: Parent

trait Rec<T>
class ARec : Rec<ARec>
class BRec : Rec<BRec>
trait SubRec<T>: Rec<T>

trait Star<T : Star<T>>
trait SubStar<T : SubStar<T>> : Star<T>