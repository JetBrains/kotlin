// IGNORE_BACKEND_FIR: JVM_IR
enum class State {
  _0,
  _1,
  _2,
  _3
}

fun box() = if(State._0.ordinal == 0 && State._1.ordinal == 1 &&  State._2.ordinal == 2 &&  State._3.ordinal == 3) "OK" else "fail"
