fun t1() : Int{
  return 0
  1
}

fun t1a() : Int {
  return
  return 1
  1
}

fun t1b() : Int {
  return 1
  return 1
  1
}

fun t1c() : Int {
  return 1
  return
  1
}

fun t2() : Int {
  if (1 > 2)
    return 1
  else return 1
  1
}

fun t2a() : Int {
  if (1 > 2) {
    return 1
    1
  } else { return 1
    2
  }
  1
}

fun t3() : Any {
  if (1 > 2)
    return 2
  else return ""
  1
}

fun t4(a : Boolean) : Int {
  do {
    return 1
  }
  while (a)
  1
}

fun t4break(a : Boolean) : Int {
  do {
    break
  }
  while (a)
  return 1
}

fun t5() : Int {
  do {
    return 1
    2
  }
  while (1 > 2)
  return 1
}

fun t6() : Int {
  while (1 > 2) {
    return 1
    2
  }
  return 1
}

fun t6break() : Int {
  while (1 > 2) {
    break
    2
  }
  return 1
}

fun t7(b : Int) : Int {
  for (i in 1..b) {
    return 1
    2
  }
  return 1
}

fun t7break(b : Int) : Int {
  for (i in 1..b) {
    return 1
    2
  }
  return 1
}

fun t7() : Int {
  try {
    return 1
    2
  }
  catch (<error descr="[TYPE_MISMATCH] Type mismatch: inferred type is kotlin/Any but kotlin/Throwable was expected">e : Any</error>) {
    2
  }
  return 1 // this is OK, like in Java
}

fun t8() : Int {
  try {
    return 1
    2
  }
  catch (<error descr="[TYPE_MISMATCH] Type mismatch: inferred type is kotlin/Any but kotlin/Throwable was expected">e : Any</error>) {
    return 1
    2
  }
  return 1
}

fun blockAndAndMismatch() : Boolean {
  (return true) || (return false)
  return true
}

fun tf() : Int {
  try {return 1} finally{return 1}
  return 1
}

fun failtest(a : Int) : Int {
  if (fail() || true) {

  }
  return 1
}

fun foo(a : Nothing) : Unit {
  1
  a
  2
}

fun fail() : Nothing {
  throw java.lang.RuntimeException()
}

fun nullIsNotNothing() : Unit {
    val x : Int? = 1
    if (x != null) {
         return
    }
    fail()
}
