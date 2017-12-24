// !WITH_NEW_INFERENCE
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
    <!DEBUG_INFO_SMARTCAST!>a<!>.bar()
    <!DEBUG_INFO_SMARTCAST!>a<!>.foo()
  }
  a?.foo()
  a?.<!UNRESOLVED_REFERENCE!>bar<!>()
  if (!(a is B)) {
    a?.<!UNRESOLVED_REFERENCE!>bar<!>()
    a?.foo()
  }
  if (!(a is B) || <!DEBUG_INFO_SMARTCAST!>a<!>.bar() == Unit) {
      a?.<!UNRESOLVED_REFERENCE!>bar<!>()
  }
  if (!(a is B)) {
    return;
  }
  <!DEBUG_INFO_SMARTCAST!>a<!>.bar()
  <!DEBUG_INFO_SMARTCAST!>a<!>.foo()
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
      <!DEBUG_INFO_SMARTCAST!>a<!>.bar();
    }
}

fun f11(a : A?) {
  when (a) {
    is B -> <!DEBUG_INFO_SMARTCAST!>a<!>.bar()
    is A -> <!DEBUG_INFO_SMARTCAST!>a<!>.foo()
    is Any -> <!DEBUG_INFO_SMARTCAST!>a<!>.foo()
    <!USELESS_IS_CHECK!>is Any?<!> -> a.<!UNRESOLVED_REFERENCE!>bar<!>()
    else -> a?.foo()
  }
}

fun f12(a : A?) {
  when (a) {
    is B -> <!DEBUG_INFO_SMARTCAST!>a<!>.bar()
    is A -> <!DEBUG_INFO_SMARTCAST!>a<!>.foo()
    is Any -> <!DEBUG_INFO_SMARTCAST!>a<!>.foo();
    <!USELESS_IS_CHECK!>is Any?<!> -> a.<!UNRESOLVED_REFERENCE!>bar<!>()
    is C -> <!DEBUG_INFO_SMARTCAST!>a<!>.bar()
    else -> a?.foo()
  }

  if (<!USELESS_IS_CHECK!>a is Any?<!>) {
    a?.<!UNRESOLVED_REFERENCE!>bar<!>()
  }
  if (a is B) {
    <!DEBUG_INFO_SMARTCAST!>a<!>.foo()
    <!DEBUG_INFO_SMARTCAST!>a<!>.bar()
  }
}

fun f13(a : A?) {
  if (a is B) {
    <!DEBUG_INFO_SMARTCAST!>a<!>.foo()
    <!DEBUG_INFO_SMARTCAST!>a<!>.bar()
  }
  else {
    a?.foo()
    <!UNRESOLVED_REFERENCE!>c<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>bar<!>()
  }

  a?.foo()
  if (!(a is B)) {
    a?.foo()
    <!UNRESOLVED_REFERENCE!>c<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>bar<!>()
  }
  else {
    <!DEBUG_INFO_SMARTCAST!>a<!>.foo()
    <!UNRESOLVED_REFERENCE!>c<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>bar<!>()
  }

  a?.foo()
  if (a is B && <!DEBUG_INFO_SMARTCAST!>a<!>.foo() == Unit) {
    <!DEBUG_INFO_SMARTCAST!>a<!>.foo()
    <!DEBUG_INFO_SMARTCAST!>a<!>.bar()
  }
  else {
    a?.foo()
    <!UNRESOLVED_REFERENCE!>c<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>bar<!>()
  }

  if (!(a is B) || !(a is C)) {
  }
  else {
  }

  if (!(a is B) || !(a is C)) {
  }

  if (!(a is B)) return
  <!DEBUG_INFO_SMARTCAST!>a<!>.bar()
}

fun f14(a : A?) {
  while (!(a is B)) {
  }
  <!DEBUG_INFO_SMARTCAST!>a<!>.bar()
}
fun f15(a : A?) {
  do {
  } while (!(a is B))
  <!DEBUG_INFO_SMARTCAST!>a<!>.bar()
}

fun getStringLength(obj : Any) : Char? {
  if (obj !is String)
    return null
  return <!DEBUG_INFO_SMARTCAST!>obj<!>.get(0) // no cast to String is needed
}

fun toInt(i: Int?): Int = if (i != null) <!DEBUG_INFO_SMARTCAST!>i<!> else 0
fun illegalWhenBody(a: Any): Int = <!NO_ELSE_IN_WHEN!>when<!>(a) {
    is Int -> <!DEBUG_INFO_SMARTCAST!>a<!>
    is String -> <!TYPE_MISMATCH!>a<!>
}
fun illegalWhenBlock(a: Any): Int {
    when(a) {
        is Int -> return <!DEBUG_INFO_SMARTCAST!>a<!>
        is String -> return <!TYPE_MISMATCH!>a<!>
    }
<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
fun declarations(a: Any?) {
    if (a is String) {
       val <!UNUSED_VARIABLE!>p4<!>: String = <!DEBUG_INFO_SMARTCAST!>a<!>
    }
    if (a is String?) {
        if (a != null) {
            val <!UNUSED_VARIABLE!>s<!>: String = <!DEBUG_INFO_SMARTCAST!>a<!>
        }
    }
    if (a != null) {
        if (a is String?) {
            val <!UNUSED_VARIABLE!>s<!>: String = <!DEBUG_INFO_SMARTCAST!>a<!>
        }
    }
}
fun vars(a: Any?) {
    var <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>b<!>: Int = 0
    if (a is Int) {
        <!UNUSED_VALUE!>b =<!> <!DEBUG_INFO_SMARTCAST!>a<!>
    }
}
fun returnFunctionLiteralBlock(a: Any?): Function0<Int> {
    if (a is Int) return { <!DEBUG_INFO_SMARTCAST!>a<!> }
    else return { 1 }
}
fun returnFunctionLiteral(a: Any?): Function0<Int> {
    if (a is Int) return { -> <!DEBUG_INFO_SMARTCAST!>a<!> }
    else return { -> 1 }
}

fun returnFunctionLiteralExpressionBody(a: Any?): Function0<Int> =
        if (a is Int) { -> <!DEBUG_INFO_SMARTCAST!>a<!> }
        else { -> 1 }


fun mergeSmartCasts(a: Any?) {
  if (a is String || a is Int) {
    a.<!UNRESOLVED_REFERENCE!>compareTo<!>("")
    <!DEBUG_INFO_SMARTCAST!>a<!>.toString()
  }
  if (a is Int || a is String) {
    a.<!UNRESOLVED_REFERENCE!>compareTo<!>("")
  }
  when (a) {
    is String, is Any -> a.<!UNRESOLVED_REFERENCE!>compareTo<!>("")
  }
  if (a is String && <!USELESS_IS_CHECK!>a is Any<!>) {
    val <!UNUSED_VARIABLE!>i<!>: Int = <!DEBUG_INFO_SMARTCAST!>a<!>.compareTo("")
  }
  if (a is String && <!DEBUG_INFO_SMARTCAST!>a<!>.compareTo("") == 0) {}
  if (a is String || a.<!UNRESOLVED_REFERENCE!>compareTo<!>("") <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>==<!> 0) {}
}

//mutability
fun f(): String {
    var a: Any = 11
    if (a is String) {
        // a is a string, despite of being a variable
        val <!UNUSED_VARIABLE!>i<!>: String = <!DEBUG_INFO_SMARTCAST!>a<!>
        <!DEBUG_INFO_SMARTCAST!>a<!>.compareTo("f")
        // Beginning from here a is captured in a closure but nobody modifies it
        val <!UNUSED_VARIABLE!>f<!>: Function0<String> = { <!DEBUG_INFO_SMARTCAST!>a<!> }
        return <!DEBUG_INFO_SMARTCAST!>a<!>
    }
    return ""
}

fun foo(aa: Any?): Int {
    var a = aa
    if (a is Int?) {
        return <!TYPE_MISMATCH!>a<!>
    }
    return 1
}
