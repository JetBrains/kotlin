fun test() {
  1 : Byte
  1 : Int
  <!TYPE_MISMATCH!>1<!> : Double
  1 <!USELESS_CAST!>as<!> Byte
  1 <!USELESS_CAST!>as<!> Int
  <!ERROR_COMPILE_TIME_VALUE!>1<!> <!CAST_NEVER_SUCCEEDS!>as<!> Double
}
