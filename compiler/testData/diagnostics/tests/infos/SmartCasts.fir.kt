open class A() {
  fun foo() {}
}

class B() : A() {
  fun bar() {}
}

fun f9(init : A?) {
  val a : A? = init
  a?.foo()
  a?.<!UNRESOLVED_REFERENCE!>bar<!>()
  if (a is B) {
    a.bar()
    a.foo()
  }
  a?.foo()
  a?.<!UNRESOLVED_REFERENCE!>bar<!>()
  if (!(a is B)) {
    a?.<!UNRESOLVED_REFERENCE!>bar<!>()
    a?.foo()
  }
  if (!(a is B) || a.bar() == Unit) {
      a?.<!UNRESOLVED_REFERENCE!>bar<!>()
  }
  if (!(a is B)) {
    return;
  }
  a.bar()
  a.foo()
}

fun f10(init : A?) {
  val a : A? = init
  if (!(a is B)) {
    return;
  }
  if (!(<!USELESS_IS_CHECK!>a is B<!>)) {
    return;
  }
}

class C() : A() {
  fun bar() {

  }
}

fun f101(a : A?) {
    if (a is C) {
      a.bar();
    }
}

fun f11(a : A?) {
  when (a) {
    is B -> a.bar()
    is A -> a.foo()
    is Any -> a.foo()
    <!USELESS_IS_CHECK!>is Any?<!> -> a.<!UNRESOLVED_REFERENCE!>bar<!>()
    else -> a?.foo()
  }
}

fun f12(a : A?) {
  when (a) {
    is B -> a.bar()
    is A -> a.foo()
    is Any -> a.foo();
    <!USELESS_IS_CHECK!>is Any?<!> -> a.<!UNRESOLVED_REFERENCE!>bar<!>()
    is C -> a.bar()
    else -> a?.foo()
  }

  if (<!USELESS_IS_CHECK!>a is Any?<!>) {
    a?.<!UNRESOLVED_REFERENCE!>bar<!>()
  }
  if (a is B) {
    a.foo()
    a.bar()
  }
}

fun f13(a : A?) {
  if (a is B) {
    a.foo()
    a.bar()
  }
  else {
    a?.foo()
    <!UNRESOLVED_REFERENCE!>c<!>.bar()
  }

  a?.foo()
  if (!(a is B)) {
    a?.foo()
    <!UNRESOLVED_REFERENCE!>c<!>.bar()
  }
  else {
    a.foo()
    <!UNRESOLVED_REFERENCE!>c<!>.bar()
  }

  a?.foo()
  if (a is B && a.foo() == Unit) {
    a.foo()
    a.bar()
  }
  else {
    a?.foo()
    <!UNRESOLVED_REFERENCE!>c<!>.bar()
  }

  if (!(a is B) || !(a is C)) {
  }
  else {
  }

  if (!(a is B) || !(a is C)) {
  }

  if (!(a is B)) return
  a.bar()
}

fun f14(a : A?) {
  while (!(a is B)) {
  }
  a.bar()
}
fun f15(a : A?) {
  do {
  } while (!(a is B))
  a.bar()
}

fun getStringLength(obj : Any) : Char? {
  if (obj !is String)
    return null
  return obj.get(0) // no cast to String is needed
}

fun toInt(i: Int?): Int = if (i != null) i else 0
fun illegalWhenBody(a: Any): Int = <!NO_ELSE_IN_WHEN!>when<!>(a) {
    is Int -> a
    is String -> a
}
fun illegalWhenBlock(a: Any): Int {
    when(a) {
        is Int -> return a
        is String -> return <!RETURN_TYPE_MISMATCH!>a<!>
    }
<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
fun declarations(a: Any?) {
    if (a is String) {
       val p4: String = a
    }
    if (a is String?) {
        if (a != null) {
            val s: String = a
        }
    }
    if (a != null) {
        if (a is String?) {
            val s: String = a
        }
    }
}
fun vars(a: Any?) {
    var b: Int = 0
    if (a is Int) {
        b = a
    }
}
fun returnFunctionLiteralBlock(a: Any?): Function0<Int> {
    if (a is Int) return { a }
    else return { 1 }
}
fun returnFunctionLiteral(a: Any?): Function0<Int> {
    if (a is Int) return { -> a }
    else return { -> 1 }
}

fun returnFunctionLiteralExpressionBody(a: Any?): Function0<Int> =
        if (a is Int) { -> a }
        else { -> 1 }


fun mergeSmartCasts(a: Any?) {
  if (a is String || a is Int) {
    a.compareTo(<!ARGUMENT_TYPE_MISMATCH!>""<!>)
    a.toString()
  }
  if (a is Int || a is String) {
    a.compareTo(<!ARGUMENT_TYPE_MISMATCH!>""<!>)
  }
  when (a) {
    is String, is Any -> a.<!UNRESOLVED_REFERENCE!>compareTo<!>("")
  }
  if (a is String && <!USELESS_IS_CHECK!>a is Any<!>) {
    val i: Int = a.compareTo("")
  }
  if (a is String && a.compareTo("") == 0) {}
  if (a is String || a.<!UNRESOLVED_REFERENCE!>compareTo<!>("") == 0) {}
}

//mutability
fun f(): String {
    var a: Any = 11
    if (a is String) {
        // a is a string, despite of being a variable
        val i: String = a
        a.compareTo("f")
        // Beginning from here a is captured in a closure but nobody modifies it
        val f: Function0<String> = { a }
        return a
    }
    return ""
}

fun foo(aa: Any?): Int {
    var a = aa
    if (a is Int?) {
        return <!RETURN_TYPE_MISMATCH!>a<!>
    }
    return 1
}
