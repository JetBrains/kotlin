<info descr="null">open</info> class A() {
  fun foo() {
      if (this is B) {
          <info descr="Implicit receiver smart cast to B">bar</info>()
      }
  }
}

class B() : A() {
  fun bar() {}
}

fun f9(a : A?) {
  a<info>?.</info>foo()
  a<info>?.</info><error descr="[UNRESOLVED_REFERENCE] Unresolved reference: bar">bar</error>()
  if (a is B) {
    <info descr="Smart cast to B">a</info>.bar()
    <info descr="Smart cast to A">a</info>.foo()
  }
  a<info>?.</info>foo()
  a<info>?.</info><error descr="[UNRESOLVED_REFERENCE] Unresolved reference: bar">bar</error>()
  if (!(a is B)) {
    a<info>?.</info><error descr="[UNRESOLVED_REFERENCE] Unresolved reference: bar">bar</error>()
    a<info>?.</info>foo()
  }
  if (!(a is B) || <info descr="Smart cast to B">a</info>.bar() == Unit) {
      a<info>?.</info><error descr="[UNRESOLVED_REFERENCE] Unresolved reference: bar">bar</error>()
  }
  if (!(a is B)) {
    return;
  }
  <info descr="Smart cast to B">a</info>.bar()
  <info descr="Smart cast to A">a</info>.foo()
}

fun fAny(a : Any?) {
    if (a is B) {
        <info descr="Smart cast to B">a</info>.bar()
        <info descr="Smart cast to B">a</info>.foo()
    }
    if (!(a is B)) {
        return;
    }
    <info descr="Smart cast to B">a</info>.bar()
    <info descr="Smart cast to B">a</info>.foo()
}

fun f10(a : A?) {
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
      <info descr="Smart cast to C">a</info>.bar();
    }
}

fun f11(a : A?) {
  when (a) {
    is B -> <info descr="Smart cast to B">a</info>.bar()
    is A -> <info descr="Smart cast to A">a</info>.foo()
    is Any -> <info descr="Smart cast to A">a</info>.foo()
    is Any? -> a.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: bar">bar</error>()
    else -> a<info>?.</info>foo()
  }
}

fun f12(a : A?) {
  when (a) {
    is B -> <info descr="Smart cast to B">a</info>.bar()
    is A -> <info descr="Smart cast to A">a</info>.foo()
    is Any -> <info descr="Smart cast to A">a</info>.foo();
    is Any? -> a.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: bar">bar</error>()
    is C -> <info descr="Smart cast to C">a</info>.bar()
    else -> a<info>?.</info>foo()
  }

  if (a is Any?) {
    a<info>?.</info><error descr="[UNRESOLVED_REFERENCE] Unresolved reference: bar">bar</error>()
  }
  if (a is B) {
    <info descr="Smart cast to B">a</info>.bar()
  }
}

fun f13(a : A?) {
  if (a is B) {
    <info descr="Smart cast to A">a</info>.foo()
    <info descr="Smart cast to B">a</info>.bar()
  }
  else {
    a<info>?.</info>foo()
    <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: c">c</error>.<error>bar</error>()
  }

  a<info>?.</info>foo()
  if (!(a is B)) {
    a<info>?.</info>foo()
  }
  else {
    <info descr="Smart cast to A">a</info>.foo()
  }

  a<info>?.</info>foo()
  if (a is B && <info descr="Smart cast to A">a</info>.foo() == Unit) {
    <info descr="Smart cast to A">a</info>.foo()
    <info descr="Smart cast to B">a</info>.bar()
  }
  else {
    a<info>?.</info>foo()
  }

  if (!(a is B) || !(a is C)) {
  }
  else {
  }

  if (!(a is B) || !(a is C)) {
  }

  if (!(a is B)) return
  <info descr="Smart cast to B">a</info>.bar()
}

fun f14(a : A?) {
  while (!(a is B)) {
  }
  <info descr="Smart cast to B">a</info>.bar()
}
fun f15(a : A?) {
  do {
  } while (!(a is B))
  <info descr="Smart cast to B">a</info>.bar()
}

fun getStringLength(obj : Any) : Char? {
  if (obj !is String)
    return null
  return <info descr="Smart cast to kotlin.String">obj</info>.get(0) // no cast to kotlin.String is needed
}

fun toInt(i: Int?): Int = if (i != null) <info descr="Smart cast to kotlin.Int">i</info> else 0
fun illegalWhenBody(a: Any): Int = when(a) {
    is Int -> <info descr="Smart cast to kotlin.Int">a</info>
    is String -> <error descr="[TYPE_MISMATCH] Type mismatch: inferred type is Any but Int was expected">a</error>
    else -> 1
}
fun illegalWhenBlock(a: Any): Int {
    when(a) {
        is Int -> return <info descr="Smart cast to kotlin.Int">a</info>
        is String -> return <error descr="[TYPE_MISMATCH] Type mismatch: inferred type is Any but Int was expected">a</error>
        else -> return 1
    }
}
fun declarations(a: Any?) {
    if (a is String) {
       val <warning>p4</warning>: String = <info descr="Smart cast to kotlin.String">a</info>
    }
    if (a is String?) {
        if (a != null) {
            val <warning>s</warning>: String = <info descr="Smart cast to kotlin.String">a</info>
        }
    }
    if (a != null) {
        if (a is String?) {
            val <warning>s</warning>: String = <info descr="Smart cast to kotlin.String">a</info>
        }
    }
}
fun vars(a: Any?) {
    var <warning>b</warning>: Int = 0
    if (a is Int) {
        <warning>b =</warning> <info descr="Smart cast to kotlin.Int">a</info>
    }
}
fun returnFunctionLiteralBlock(a: Any?): Function0<Int> {
    if (<info>a</info> is Int) return { <info descr="Smart cast to kotlin.Int"><info>a</info></info> }
    else return { 1 }
}
fun returnFunctionLiteral(a: Any?): Function0<Int> =
    if (<info>a</info> is Int) (fun (): Int = <info descr="Smart cast to kotlin.Int"><info>a</info></info>)
    else { -> 1 }

fun mergeSmartCasts(a: Any?) {
  if (a is String || a is Int) {
    a.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: compareTo">compareTo</error>("")
    <info descr="Smart cast to kotlin.Any"><info>a</info></info>.toString()
  }
  if (a is Int || a is String) {
    a.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: compareTo">compareTo</error>("")
  }
  when (a) {
    is String, is Any -> a.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: compareTo">compareTo</error>("")
  }
  if (a is String && a is Any) {
    val <warning>i</warning>: Int = <info descr="Smart cast to kotlin.String">a</info>.compareTo("")
  }
  if (a is String && <info descr="Smart cast to kotlin.String">a</info>.compareTo("") == 0) {}
  if (a is String || a.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: compareTo">compareTo</error>("") <error>==</error> 0) {}
}

//mutability
fun f(): String {
    var a: Any = 11
    if (<info>a</info> is String) {
        val <warning>i</warning>: String = <info descr="Smart cast to kotlin.String"><info descr="Wrapped into a reference object to be modified when captured in a closure">a</info></info>
        <info descr="Smart cast to kotlin.String"><info descr="Wrapped into a reference object to be modified when captured in a closure">a</info></info>.compareTo("f")
        val <warning>f</warning>: Function0<String> = {
            <info>a</info> = 42
            <error descr="[TYPE_MISMATCH] Type mismatch: inferred type is Any but String was expected">a</error>
        }
        return <error descr="[SMARTCAST_IMPOSSIBLE] Smart cast to 'String' is impossible, because 'a' is a local variable that is captured by a changing closure">a</error>
    }
    return ""
}

class Mutable(var x: String?) {

    val xx: String?
        <info descr="null">get</info>() = x

    fun foo(): String {
        if (x is String) {
            return <error descr="[SMARTCAST_IMPOSSIBLE] Smart cast to 'String' is impossible, because 'x' is a mutable property that could have been changed by this time">x</error>
        }
        if (x != null) {
            return <error descr="[SMARTCAST_IMPOSSIBLE] Smart cast to 'String' is impossible, because 'x' is a mutable property that could have been changed by this time">x</error>
        }
        if (xx is String) {
            return <error descr="[SMARTCAST_IMPOSSIBLE] Smart cast to 'String' is impossible, because 'xx' is a property that has open or custom getter">xx</error>
        }
        return ""
    }

    fun bar(other: Mutable): String {
        var y = other
        if (y.x is String) {
            return <error descr="[SMARTCAST_IMPOSSIBLE] Smart cast to 'String' is impossible, because 'y.x' is a complex expression">y.x</error>
        }
        return ""
    }
}

fun foo(aa: Any): Int {
    var a = aa
    if (a is Int) {
        return <info descr="Smart cast to kotlin.Int">a</info>
    }
    return 1
}

fun inForLoop(x: Any?) {
    if (x is Array<*>) {
        for (i in <info descr="Smart cast to kotlin.Array<*>">x</info>) {}
    }
    for (i in <error descr="[ITERATOR_MISSING] For-loop range must have an 'iterator()' method">x</error>) {}
}

class ExplicitAccessorForAnnotation {
    val tt: String? = "good"
        <info descr="null">get</info>

    fun foo(): String {
        if (tt is String) {
            return <error descr="[SMARTCAST_IMPOSSIBLE] Smart cast to 'String' is impossible, because 'tt' is a property that has open or custom getter">tt</error>
        }
        return ""
    }
}