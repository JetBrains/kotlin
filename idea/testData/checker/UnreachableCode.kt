fun t1() : Int{
  return 0
  <error>1</error>
}

fun t1a() : Int {
  <error>return</error>
  <error>return 1</error>
  <error>1</error>
}

fun t1b() : Int {
  return 1
  <error>return 1</error>
  <error>1</error>
}

fun t1c() : Int {
  return 1
  <error>return</error>
  <error>1</error>
}

fun t2() : Int {
  if (1 > 2)
    return 1
  else return 1
  <error>1</error>
}

fun t2a() : Int {
  if (1 > 2) {
    return 1
    <error>1</error>
  } else { return 1
    <error>2</error>
  }
  <error>1</error>
}

fun t3() : Any {
  if (1 > 2)
    return 2
  else return ""
  <error>1</error>
}

fun t4(<warning>a</warning> : Boolean) : Int {
  do {
    return 1
  }
  while (<error>a</error>)
  <error>1</error>
}

fun t4break(<warning>a</warning> : Boolean) : Int {
  do {
    break
  }
  while (<error>a</error>)
  return 1
}

fun t5() : Int {
  do {
    return 1
    <error>2</error>
  }
  while (<error>1 > 2</error>)
  <error>return 1</error>
}

fun t6() : Int {
  while (1 > 2) {
    return 1
    <error>2</error>
  }
  return 1
}

fun t6break() : Int {
  while (1 > 2) {
    break
    <error>2</error>
  }
  return 1
}

fun t7(b : Int) : Int {
  for (i in 1..b) {
    return 1
    <error>2</error>
  }
  return 1
}

fun t7break(b : Int) : Int {
  for (i in 1..b) {
    return 1
    <error>2</error>
  }
  return 1
}

fun t7() : Int {
  try {
    return 1
    <error>2</error>
  }
  catch (<error>e : Any</error>) {
    <warning>2</warning>
  }
  return 1 // this is OK, like in Java
}

fun t8() : Int {
  try {
    return 1
    <error>2</error>
  }
  catch (<error>e : Any</error>) {
    return 1
    <error>2</error>
  }
  <error>return 1</error>
}

fun blockAndAndMismatch() : Boolean {
  <error><warning>(return true)</warning> || (return false)</error>
  <error>return true</error>
}

fun tf() : Int {
  try {<error>return 1</error>} finally{return 1}
  <error>return 1</error>
}

fun failtest(<warning>a</warning> : Int) : Int {
  if (fail() || <error>true</error>) {

  }
  <error>return 1</error>
}

fun foo(a : Nothing) : Unit {
  <warning>1</warning>
  <warning>a</warning>
  <error>2</error>
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