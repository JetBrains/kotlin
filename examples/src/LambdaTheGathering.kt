package LambdaTheGathering

/**
 * Incomplete solution to the IFCP Contest 2011
 * @author yole
 */

class GameError(msg: String): Exception(msg) {
}

abstract class Function(val name: String) {
  abstract fun apply(x: Value, proponent: Player): Value
  fun toString() = name
  open fun asNumber(): Int { throw GameError("could not get numeric value of a function") }
}

object Identity: Function("I") {
  override fun apply(x: Value, proponent: Player): Value = x
}

object IdentityValue: Value(Identity)

object Zero: Function("zero") {
  override fun apply(x: Value, proponent: Player): Value = Value(0)
  override fun asNumber(): Int = 0
}

object Succ: Function("succ") {
  override fun apply(x: Value, proponent: Player): Value = Value(x.asNumber() + 1)
}

object Get: Function("get") {
  override fun apply(x: Value, proponent: Player): Value = proponent.getSlot(x.asNumber()).field
}

object Dbl: Function("dbl") {
  override fun apply(x: Value, proponent: Player): Value = Value(x.asNumber() * 2)
}

class HelpFunc2(val i: Value, val j: Value): Function("help(" + i + ")(" + j + ")") {
  override fun apply(x: Value, proponent: Player): Value {
    val n = x.asNumber()
    proponent.getSlot(i.asNumber()).decreaseVitality(n)
    proponent.getSlot(j.asNumber()).increaseVitality(n * 11 / 10);
    return Value(Identity)
  }
}

class HelpFunc(val i: Value): Function("help(" + i + ")") {
  override fun apply(x: Value, proponent: Player): Value = Value(HelpFunc2(i, x))
}

object Help: Function("help") {
  override fun apply(x: Value, proponent: Player): Value = Value(HelpFunc(x))
}

class KFunc(val arg: Value): Function("K(" + arg + ")") {
  override fun apply(x: Value, proponent: Player): Value = arg
}

object K: Function("K") {
  override fun apply(x: Value, proponent: Player): Value = Value(KFunc(x))
}

class SFunc2(val f: Value, val g: Value): Function("S(" + f + ")(" + g + ")") {
  override fun apply(x: Value, proponent: Player): Value {
    val h = f.apply(x, proponent)
    val y = g.apply(x, proponent)
    val z = h.apply(y, proponent)
    return z
  }
}

class SFunc(val f: Value): Function("S(" + f + ")") {
  override fun apply(x: Value, proponent: Player): Value = Value(SFunc2(f, x))
}

object S: Function("S") {
  override fun apply(x: Value, proponent: Player): Value = Value(SFunc(x))
}

open class Value(val n: Int, val f: Function, val isFunction: Boolean) {
  this(n: Int): this(if (n > 65535) 65535 else n, Identity, false) {}
  this(f: Function): this(0, f, true) {}

  fun equals(rhs: Any?): Boolean {
    if (rhs is Value) {
      val rhsValue = rhs as Value
      if (rhsValue.isFunction) {
        return isFunction && f === rhsValue.f
      }
      else {
        return ! isFunction && n == rhsValue.n
      }
    }
    return false
  }

  fun apply(x: Value, proponent: Player): Value {
    if (isFunction) return f.apply(x, proponent)
    throw GameError("could not apply non-function value");
  }

  fun asNumber(): Int {
    if (! isFunction) return n;
    return f.asNumber()
  }

  fun toString() = if (isFunction) f.name else Integer.toString(n)
}

class Slot() {
  var vitality: Int = 10000
  var field: Value = Value(Identity)

  val alive: Boolean get() = vitality > 0

  fun isDefault() = vitality == 10000 && field == Value(Identity)

  fun increaseVitality(delta: Int) {
    if (alive) {
      vitality += delta
      if (vitality > 65535) vitality = 65535;
    }
  }

  fun decreaseVitality(delta: Int) {
    if (!alive || delta > vitality) throw GameError("cannot decrease vitality")
    vitality -= delta
  }

  fun toString() = "{" + vitality + "," + field.toString() + "}";
}

class Player() {
  var opponent: Player? = null

  val slots: Array<Slot> = Array<Slot>(256, { i -> Slot() })

  fun getSlot(i: Int): Slot {
    if (i < 0 || i > 255) throw GameError("invalid slot index")
    if (!slots[i].alive) throw GameError("the slot is dead")
    return slots[i]
  }

  fun printSlots() {
    println("(slots {10000,I} are omitted)")
    for(val i in 0..255) {
      if (!slots[i].isDefault()) {
        println("" + i + "=" + slots[i].toString())
      }
    }
  }

  fun applyCardToSlot(index: Int, card: Function) {
    println("applying card " + card + " to slot " + index)
    slots[index].field = card.apply(slots[index].field, this)
  }

  fun applySlotToCard(index: Int, card: Function) {
    println("applying slot " + index + " to card " + card)
    slots[index].field = slots[index].field.apply(Value(card), this)
  }
}

val p0 = Player()
val p1 = Player()

fun main(args: Array<String>) {
  p0.opponent = p1
  p1.opponent = p0

  print("player 0")
  p0.printSlots()
  p0.applySlotToCard(0, Help)
  p0.printSlots()
  p0.applySlotToCard(0, Zero)
  p0.printSlots()
  p0.applyCardToSlot(0, K)
  p0.printSlots()
  p0.applyCardToSlot(0, S)
  p0.printSlots()
  p0.applySlotToCard(0, Succ)
  p0.printSlots()
  p0.applySlotToCard(0, Zero)
  p0.printSlots()
  p0.applySlotToCard(1, Zero)
  p0.printSlots()
  p0.applyCardToSlot(1, Succ)
  p0.printSlots()
  p0.applyCardToSlot(1, Dbl)
  p0.printSlots()
  p0.applyCardToSlot(1, Dbl)
  p0.printSlots()
  p0.applyCardToSlot(1, Dbl)
  p0.printSlots()
  p0.applyCardToSlot(1, Dbl)
  p0.printSlots()
  p0.applyCardToSlot(0, K)
  p0.printSlots()
  p0.applyCardToSlot(0, S)
  p0.printSlots()
  p0.applySlotToCard(0, Get)
  p0.printSlots()
  p0.applyCardToSlot(0, K)
  p0.printSlots()
  p0.applyCardToSlot(0, S)
  p0.printSlots()
  p0.applySlotToCard(0, Succ)
  p0.printSlots()
  p0.applySlotToCard(0, Zero)
  p0.printSlots()
}
