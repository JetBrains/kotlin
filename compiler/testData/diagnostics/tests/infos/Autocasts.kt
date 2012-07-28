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
  if (!(a is B) || a.bar() == #()) {
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
  if (!(a is B)) {
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
    is Any? -> a.<!UNRESOLVED_REFERENCE!>bar<!>()
    else -> a?.foo()
  }
}

fun f12(a : A?) {
  when (a) {
    is B -> a.bar()
    is A -> a.foo()
    is Any -> a.foo();
    is Any? -> a.<!UNRESOLVED_REFERENCE!>bar<!>()
    is val c : <!TYPE_MISMATCH_IN_BINDING_PATTERN!>B<!> -> c.foo()
    is val c is C -> c.bar()
    is val c is C -> a.bar()
    else -> a?.foo()
  }

  if (a is val b) {
    a?.<!UNRESOLVED_REFERENCE!>bar<!>()
    b?.foo()
  }
  if (a is val b is B) {
    b.foo()
    a.bar()
    b.bar()
  }
}

fun f13(a : A?) {
  if (a is val c is B) {
    c.foo()
    c.bar()
  }
  else {
    a?.foo()
    <!UNRESOLVED_REFERENCE!>c<!>.bar()
  }

  a?.foo()
  if (!(a is val c is B)) {
    a?.foo()
    <!UNRESOLVED_REFERENCE!>c<!>.bar()
  }
  else {
    a.foo()
    <!UNRESOLVED_REFERENCE!>c<!>.bar()
  }

  a?.foo()
  if (a is val c is B && a.foo() == #() && c.bar() == #()) {
    c.foo()
    c.bar()
  }
  else {
    a?.foo()
    <!UNRESOLVED_REFERENCE!>c<!>.bar()
  }

  if (!(a is val c is B) || !(a is val x is C)) {
    <!UNRESOLVED_REFERENCE, UNUSED_EXPRESSION!>x<!>
    <!UNRESOLVED_REFERENCE, UNUSED_EXPRESSION!>c<!>
  }
  else {
    <!UNRESOLVED_REFERENCE, UNUSED_EXPRESSION!>x<!>
    <!UNRESOLVED_REFERENCE, UNUSED_EXPRESSION!>c<!>
  }

  if (!(a is val c is B) || !(a is val c is C)) {
  }

  if (!(a is val c is B)) return
  a.bar()
  <!UNRESOLVED_REFERENCE!>c<!>.foo()
  <!UNRESOLVED_REFERENCE!>c<!>.bar()
}

fun f14(a : A?) {
  while (!(a is val c is B)) {
  }
  a.bar()
  <!UNRESOLVED_REFERENCE!>c<!>.bar()
}
fun f15(a : A?) {
  do {
  } while (!(a is val c is B))
  a.bar()
  <!UNRESOLVED_REFERENCE!>c<!>.bar()
}

fun getStringLength(obj : Any) : Char? {
  if (obj !is String)
    return null
  return obj.get(0) // no cast to String is needed
}

fun toInt(i: Int?): Int = if (i != null) i else 0
fun illegalWhenBody(a: Any): Int = <!NO_ELSE_IN_WHEN!>when<!>(a) {
    is Int -> a
    is String -> <!TYPE_MISMATCH!>a<!>
}
fun illegalWhenBlock(a: Any): Int {
    <!NO_ELSE_IN_WHEN!>when<!>(a) {
        is Int -> return a
        is String -> return <!TYPE_MISMATCH!>a<!>
    }
}
fun declarations(a: Any?) {
    if (a is String) {
       val <!UNUSED_VARIABLE!>p4<!>: #(Int, String) = #(2, a)
    }
    if (a is String?) {
        if (a != null) {
            val <!UNUSED_VARIABLE!>s<!>: String = a
        }
    }
    if (a != null) {
        if (a is String?) {
            val <!UNUSED_VARIABLE!>s<!>: String = a
        }
    }
}
fun vars(a: Any?) {
    var <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>b<!>: Int = 0
    if (a is Int) {
        b = <!UNUSED_VALUE!>a<!>
    }
}
fun tuples(a: Any?) {
    if (a != null) {
        val <!UNUSED_VARIABLE!>s<!>: #(Any, String) = #(a, <!TYPE_MISMATCH!>a<!>)
    }
    if (a is String) {
        val <!UNUSED_VARIABLE!>s<!>: #(Any, String) = #(a, a)
    }
    fun illegalTupleReturnType(): #(Any, String) = #(<!TYPE_MISMATCH!>a<!>, <!TYPE_MISMATCH!>a<!>)
    if (a is String) {
        fun legalTupleReturnType(): #(Any, String) = #(a, a)
    }
    val <!UNUSED_VARIABLE!>illegalFunctionLiteral<!>: Function0<Int> = <!TYPE_MISMATCH!>{ <!TYPE_MISMATCH!>a<!> }<!>
    val <!UNUSED_VARIABLE!>illegalReturnValueInFunctionLiteral<!>: Function0<Int> = { (): Int -> <!TYPE_MISMATCH!>a<!> }

    if (a is Int) {
        val <!UNUSED_VARIABLE!>legalFunctionLiteral<!>: Function0<Int> = { a }
        val <!UNUSED_VARIABLE!>alsoLegalFunctionLiteral<!>: Function0<Int> = { (): Int -> a }
    }
}
fun returnFunctionLiteralBlock(a: Any?): Function0<Int> {
    if (a is Int) return { a }
    else return { 1 }
}
fun returnFunctionLiteral(a: Any?): Function0<Int> =
    if (a is Int) { (): Int -> a }
    else { () -> 1 }

fun illegalTupleReturnType(a: Any): #(Any, String) = #(a, <!TYPE_MISMATCH!>a<!>)

fun declarationInsidePattern(x: #(Any, Any)): String = when(x) { is #(val a is String, *) -> a; else -> "something" }

fun mergeAutocasts(a: Any?) {
  if (a is String || a is Int) {
    a.<!UNRESOLVED_REFERENCE!>compareTo<!>("")
    a.toString()
  }
  if (a is Int || a is String) {
    a.<!UNRESOLVED_REFERENCE!>compareTo<!>("")
  }
  <!NO_ELSE_IN_WHEN!>when<!> (a) {
    is String, is Any -> a.<!UNRESOLVED_REFERENCE!>compareTo<!>("")
  }
  if (a is String && a is Any) {
    val <!UNUSED_VARIABLE!>i<!>: Int = a.compareTo("")
  }
  if (a is String && a.compareTo("") == 0) {}
  if (a is String || a.<!UNRESOLVED_REFERENCE!>compareTo<!>("") == 0) {}
}

//mutability
fun f(): String {
    var a: Any = 11
    if (a is String) {
        val <!UNUSED_VARIABLE!>i<!>: String = <!AUTOCAST_IMPOSSIBLE!>a<!>
        <!AUTOCAST_IMPOSSIBLE!>a<!>.compareTo("f")
        val <!UNUSED_VARIABLE!>f<!>: Function0<String> = { <!AUTOCAST_IMPOSSIBLE!>a<!> }
        return <!AUTOCAST_IMPOSSIBLE!>a<!>
    }
    return ""
}

fun foo(var a: Any): Int {
    if (a is Int) {
        return <!AUTOCAST_IMPOSSIBLE!>a<!>
    }
    return 1
}
