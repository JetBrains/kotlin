fun test() {
  val a : Int? = 0
  if (a != null) {
    <!DEBUG_INFO_AUTOCAST!>a<!>.plus(1)
  }
  else {
    a?.plus(1)
  }

  val out : java.io.PrintStream? = null
  val ins : java.io.InputStream? = null

  out?.println()
  ins?.read()

  if (ins != null) {
    <!DEBUG_INFO_AUTOCAST!>ins<!>.read()
    out?.println()
    if (out != null) {
      <!DEBUG_INFO_AUTOCAST!>ins<!>.read();
      <!DEBUG_INFO_AUTOCAST!>out<!>.println();
    }
  }

  if (out != null && ins != null) {
    <!DEBUG_INFO_AUTOCAST!>ins<!>.read();
    <!DEBUG_INFO_AUTOCAST!>out<!>.println();
  }

  if (out == null) {
    out?.println()
  } else {
    <!DEBUG_INFO_AUTOCAST!>out<!>.println()
  }

  if (out != null && ins != null || out != null) {
    ins?.read();
    ins<!UNSAFE_CALL!>.<!>read();
    <!DEBUG_INFO_AUTOCAST!>out<!>.println();
  }

  if (out == null || <!DEBUG_INFO_AUTOCAST!>out<!>.println(0) == Unit) {
    out?.println(1)
    out<!UNSAFE_CALL!>.<!>println(1)
  }
  else {
    <!DEBUG_INFO_AUTOCAST!>out<!>.println(2)
  }

  if (out != null && <!DEBUG_INFO_AUTOCAST!>out<!>.println() == Unit) {
    <!DEBUG_INFO_AUTOCAST!>out<!>.println();
  }
  else {
    out?.println();
  }

  if (out == null || <!DEBUG_INFO_AUTOCAST!>out<!>.println() == Unit) {
    out?.println();
  }
  else {
    <!DEBUG_INFO_AUTOCAST!>out<!>.println();
  }

  if (1 == 2 || out != null && <!DEBUG_INFO_AUTOCAST!>out<!>.println(1) == Unit) {
    out?.println(2);
    out<!UNSAFE_CALL!>.<!>println(2);
  }
  else {
    out?.println(3)
    out<!UNSAFE_CALL!>.<!>println(3)
  }

  out?.println()
  ins?.read()

  if (ins != null) {
    <!DEBUG_INFO_AUTOCAST!>ins<!>.read()
    out?.println()
    if (out != null) {
      <!DEBUG_INFO_AUTOCAST!>ins<!>.read();
      <!DEBUG_INFO_AUTOCAST!>out<!>.println();
    }
  }

  if (out != null && ins != null) {
    <!DEBUG_INFO_AUTOCAST!>ins<!>.read();
    <!DEBUG_INFO_AUTOCAST!>out<!>.println();
  }

  if (out == null) {
    out?.println()
  } else {
    <!DEBUG_INFO_AUTOCAST!>out<!>.println()
  }

  if (out != null && ins != null || out != null) {
    ins?.read();
    <!DEBUG_INFO_AUTOCAST!>out<!>.println();
  }

  if (out == null || <!DEBUG_INFO_AUTOCAST!>out<!>.println(0) == Unit) {
    out?.println(1)
    out<!UNSAFE_CALL!>.<!>println(1)
  }
  else {
    <!DEBUG_INFO_AUTOCAST!>out<!>.println(2)
  }

  if (out != null && <!DEBUG_INFO_AUTOCAST!>out<!>.println() == Unit) {
    <!DEBUG_INFO_AUTOCAST!>out<!>.println();
  }
  else {
    out?.println();
    out<!UNSAFE_CALL!>.<!>println();
  }

  if (out == null || <!DEBUG_INFO_AUTOCAST!>out<!>.println() == Unit) {
    out?.println();
    out<!UNSAFE_CALL!>.<!>println();
  }
  else {
    <!DEBUG_INFO_AUTOCAST!>out<!>.println();
  }

  if (1 == 2 || out != null && <!DEBUG_INFO_AUTOCAST!>out<!>.println(1) == Unit) {
    out?.println(2);
    out<!UNSAFE_CALL!>.<!>println(2);
  }
  else {
    out?.println(3)
    out<!UNSAFE_CALL!>.<!>println(3)
  }

  if (1 > 2) {
    if (out == null) return;
    <!DEBUG_INFO_AUTOCAST!>out<!>.println();
  }
  out?.println();

  while (out != null) {
    <!DEBUG_INFO_AUTOCAST!>out<!>.println();
  }
  out?.println();

  val out2 : java.io.PrintStream? = null
  
  while (out2 == null) {
    out2?.println();
    out2<!UNSAFE_CALL!>.<!>println();
  }
  <!DEBUG_INFO_AUTOCAST!>out2<!>.println()

}


fun f(out : String?) {
  out?.get(0)
  out<!UNSAFE_CALL!>.<!>get(0)
  if (out != null) else return;
  <!DEBUG_INFO_AUTOCAST!>out<!>.get(0)
}

fun f1(out : String?) {
  out?.get(0)
  if (out != null) else {
    1 + 2
    return;
  }
  <!DEBUG_INFO_AUTOCAST!>out<!>.get(0)
}

fun f2(out : String?) {
  out?.get(0)
  if (out == null) {
    1 + 2
    return;
  }
  <!DEBUG_INFO_AUTOCAST!>out<!>.get(0)
}

fun f3(out : String?) {
  out?.get(0)
  if (out == null) {
    1 + 2
    return;
  }
  else {
    1 + 2
  }
  <!DEBUG_INFO_AUTOCAST!>out<!>.get(0)
}

fun f4(s : String?) {
  s?.get(0)
  while (1 < 2 && s != null) {
    <!DEBUG_INFO_AUTOCAST!>s<!>.get(0)
  }
  s?.get(0)
  while (s == null || 1 < 2) {
     s?.get(0)
  }
  <!DEBUG_INFO_AUTOCAST!>s<!>.get(0)
}

fun f5(s : String?) {
  s?.get(0)
  while (1 < 2 && s != null) {
    <!DEBUG_INFO_AUTOCAST!>s<!>.get(0)
  }
  s?.get(0)
  while (s == null || 1 < 2) {
    if (1 > 2) break
     s?.get(0)
  }
  s?.get(0);
}

fun f6(s : String?) {
  s?.get(0)
  do {
    s?.get(0)
    if (1 < 2) break;
  } while (s == null)
  s?.get(0)
  do {
    s?.get(0)
  } while (s == null)
  <!DEBUG_INFO_AUTOCAST!>s<!>.get(0)
}

fun f7(s : String?, t : String?) {
  s?.get(0)
  if (!(s == null)) {
    <!DEBUG_INFO_AUTOCAST!>s<!>.get(0)
  }
  s?.get(0)
  if (!(s != null)) {
    s?.get(0)
  }
  else {
    <!DEBUG_INFO_AUTOCAST!>s<!>.get(0)
  }
  s?.get(0)
  if (!!(s != null)) {
    <!DEBUG_INFO_AUTOCAST!>s<!>.get(0)
  }
  else {
    s?.get(0)
  }
  s?.get(0)
  t?.get(0)
  if (!(s == null || t == null)) {
    <!DEBUG_INFO_AUTOCAST!>s<!>.get(0)
    <!DEBUG_INFO_AUTOCAST!>t<!>.get(0)
  }
  else {
    s?.get(0)
    t?.get(0)
  }
  s?.get(0)
  t?.get(0)
  if (!(s == null)) {
    <!DEBUG_INFO_AUTOCAST!>s<!>.get(0)
    t?.get(0)
  }
  else {
    s?.get(0)
    t?.get(0)
  }
}

fun f8(b : String?, a : String) {
  b?.get(0)
  if (b == a) {
    <!DEBUG_INFO_AUTOCAST!>b<!>.get(0);
  }
  b?.get(0)
  if (a == b) {
    <!DEBUG_INFO_AUTOCAST!>b<!>.get(0)
  }
  if (a != b) {
    b?.get(0)
  }
  else {
    <!DEBUG_INFO_AUTOCAST!>b<!>.get(0)
  }
}

fun f9(a : Int?) : Int {
  if (a != null)
    return <!DEBUG_INFO_AUTOCAST!>a<!>
  return 1
}
