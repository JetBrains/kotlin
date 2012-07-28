
fun t1() : Int{
  return 0
  <!UNREACHABLE_CODE!>1<!>
}

fun t1a() : Int {
  <!RETURN_TYPE_MISMATCH!>return<!>
  <!UNREACHABLE_CODE!>return 1<!>
  <!UNREACHABLE_CODE!>1<!>
}

fun t1b() : Int {
  return 1
  <!UNREACHABLE_CODE!>return 1<!>
  <!UNREACHABLE_CODE!>1<!>
}

fun t1c() : Int {
  return 1
  <!RETURN_TYPE_MISMATCH, UNREACHABLE_CODE!>return<!>
  <!UNREACHABLE_CODE!>1<!>
}

fun t2() : Int {
  if (1 > 2)
    return 1
  else return 1
  <!UNREACHABLE_CODE!>1<!>
}

fun t2a() : Int {
  if (1 > 2) {
    return 1
    <!UNREACHABLE_CODE!>1<!>
  } else { return 1
    <!UNREACHABLE_CODE!>2<!>
  }
  <!UNREACHABLE_CODE!>1<!>
}

fun t3() : Any {
  if (1 > 2)
    return 2
  else return ""
  <!UNREACHABLE_CODE!>1<!>
}

fun t4(<!UNUSED_PARAMETER!>a<!> : Boolean) : Int {
  do {
    return 1
  }
  while (<!UNREACHABLE_CODE!>a<!>)
  <!UNREACHABLE_CODE!>1<!>
}

fun t4break(<!UNUSED_PARAMETER!>a<!> : Boolean) : Int {
  do {
    break
  }
  while (<!UNREACHABLE_CODE!>a<!>)
  return 1
}

fun t5() : Int {
  do {
    return 1
    <!UNREACHABLE_CODE!>2<!>
  }
  while (<!UNREACHABLE_CODE!>1 > 2<!>)
  <!UNREACHABLE_CODE!>return 1<!>
}

fun t6() : Int {
  while (1 > 2) {
    return 1
    <!UNREACHABLE_CODE!>2<!>
  }
  return 1
}

fun t6break() : Int {
  while (1 > 2) {
    break
    <!UNREACHABLE_CODE!>2<!>
  }
  return 1
}

fun t7(b : Int) : Int {
  for (i in 1..b) {
    return 1
    <!UNREACHABLE_CODE!>2<!>
  }
  return 1
}

fun t7break(b : Int) : Int {
  for (i in 1..b) {
    return 1
    <!UNREACHABLE_CODE!>2<!>
  }
  return 1
}

fun t7() : Int {
  try {
    return 1
    <!UNREACHABLE_CODE!>2<!>
  }
  catch (<!TYPE_MISMATCH!>e : Any<!>) {
    <!UNUSED_EXPRESSION!>2<!>
  }
  return 1 // this is OK, like in Java
}

fun t8() : Int {
  try {
    return 1
    <!UNREACHABLE_CODE!>2<!>
  }
  catch (<!TYPE_MISMATCH!>e : Any<!>) {
    return 1
    <!UNREACHABLE_CODE!>2<!>
  }
  <!UNREACHABLE_CODE!>return 1<!>
}

fun blockAndAndMismatch() : Boolean {
  <!UNREACHABLE_CODE!>(return true) || (return false)<!>
  <!UNREACHABLE_CODE!>return true<!>
}

fun tf() : Int {
  try {<!UNREACHABLE_CODE!>return 1<!>} finally{return 1}
  <!UNREACHABLE_CODE!>return 1<!>
}

fun failtest(<!UNUSED_PARAMETER!>a<!> : Int) : Int {
  if (fail() || <!UNREACHABLE_CODE!>true<!>) {

  }
  <!UNREACHABLE_CODE!>return 1<!>
}

fun foo(a : Nothing) : Unit {
  <!UNUSED_EXPRESSION!>1<!>
  <!UNUSED_EXPRESSION!>a<!>
  <!UNREACHABLE_CODE!>2<!>
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

fun returnInWhile(<!UNUSED_PARAMETER!>a<!>: Int) {
    do {return}
    while (<!UNREACHABLE_CODE!>1 > a<!>)
}
