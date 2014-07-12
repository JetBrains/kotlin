<info descr="null">open</info> class A() {
  fun foo() {}
}

class B() : A() {
  fun bar() {}
}

fun f9(a : A?) {
  a<info>?.</info>foo()
  a<info>?.</info><error descr="[UNRESOLVED_REFERENCE] Unresolved reference: bar">bar</error>()
  if (a is B) {
    <info descr="Automatically cast to B">a</info>.bar()
    <info descr="Automatically cast to B">a</info>.foo()
  }
  a<info>?.</info>foo()
  a<info>?.</info><error descr="[UNRESOLVED_REFERENCE] Unresolved reference: bar">bar</error>()
  if (!(a is B)) {
    a<info>?.</info><error descr="[UNRESOLVED_REFERENCE] Unresolved reference: bar">bar</error>()
    a<info>?.</info>foo()
  }
  if (!(a is B) || <info descr="Automatically cast to B">a</info>.bar() == Unit) {
      a<info>?.</info><error descr="[UNRESOLVED_REFERENCE] Unresolved reference: bar">bar</error>()
  }
  if (!(a is B)) {
    return;
  }
  <info descr="Automatically cast to B">a</info>.bar()
  <info descr="Automatically cast to B">a</info>.foo()
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
      <info descr="Automatically cast to C">a</info>.bar();
    }
}

fun f11(a : A?) {
  when (a) {
    is B -> <info descr="Automatically cast to B">a</info>.bar()
    is A -> <info descr="Automatically cast to A">a</info>.foo()
    is Any -> <info descr="Automatically cast to A">a</info>.foo()
    is Any? -> a.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: bar">bar</error>()
    else -> a<info>?.</info>foo()
  }
}

fun f12(a : A?) {
  when (a) {
    is B -> <info descr="Automatically cast to B">a</info>.bar()
    is A -> <info descr="Automatically cast to A">a</info>.foo()
    is Any -> <info descr="Automatically cast to A">a</info>.foo();
    is Any? -> a.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: bar">bar</error>()
    is C -> <info descr="Automatically cast to C">a</info>.bar()
    else -> a<info>?.</info>foo()
  }

  if (a is Any?) {
    a<info>?.</info><error descr="[UNRESOLVED_REFERENCE] Unresolved reference: bar">bar</error>()
  }
  if (a is B) {
    <info descr="Automatically cast to B">a</info>.bar()
  }
}

fun f13(a : A?) {
  if (a is B) {
    <info descr="Automatically cast to B">a</info>.foo()
    <info descr="Automatically cast to B">a</info>.bar()
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
    <info descr="Automatically cast to B">a</info>.foo()
  }

  a<info>?.</info>foo()
  if (a is B && <info descr="Automatically cast to B">a</info>.foo() == Unit) {
    <info descr="Automatically cast to B">a</info>.foo()
    <info descr="Automatically cast to B">a</info>.bar()
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
  <info descr="Automatically cast to B">a</info>.bar()
}

fun f14(a : A?) {
  while (!(a is B)) {
  }
  <info descr="Automatically cast to B">a</info>.bar()
}
fun f15(a : A?) {
  do {
  } while (!(a is B))
  <info descr="Automatically cast to B">a</info>.bar()
}

fun getStringLength(obj : Any) : Char? {
  if (obj !is String)
    return null
  return <info descr="Automatically cast to kotlin.String">obj</info>.get(0) // no cast to kotlin.String is needed
}

fun toInt(i: Int?): Int = if (i != null) <info descr="Automatically cast to kotlin.Int">i</info> else 0
fun illegalWhenBody(a: Any): Int = when(a) {
    is Int -> <info descr="Automatically cast to kotlin.Int">a</info>
    is String -> <error descr="[TYPE_MISMATCH] Type mismatch: inferred type is kotlin.Any but kotlin.Int was expected">a</error>
    else -> 1
}
fun illegalWhenBlock(a: Any): Int {
    when(a) {
        is Int -> return <info descr="Automatically cast to kotlin.Int">a</info>
        is String -> return <error descr="[TYPE_MISMATCH] Type mismatch: inferred type is kotlin.Any but kotlin.Int was expected">a</error>
        else -> return 1
    }
}
fun declarations(a: Any?) {
    if (a is String) {
       val <warning>p4</warning>: String = <info descr="Automatically cast to kotlin.String">a</info>
    }
    if (a is String?) {
        if (a != null) {
            val <warning>s</warning>: String = <info descr="Automatically cast to kotlin.String">a</info>
        }
    }
    if (a != null) {
        if (a is String?) {
            val <warning>s</warning>: String = <info descr="Automatically cast to kotlin.String">a</info>
        }
    }
}
fun vars(a: Any?) {
    var <warning>b</warning>: Int = 0
    if (a is Int) {
        b = <warning><info descr="Automatically cast to kotlin.Int">a</info></warning>
    }
}
fun returnFunctionLiteralBlock(<info>a</info>: Any?): Function0<Int> {
    if (<info>a</info> is Int) return { <info descr="Automatically cast to kotlin.Int"><info>a</info></info> }
    else return { 1 }
}
fun returnFunctionLiteral(<info>a</info>: Any?): Function0<Int> =
    if (<info>a</info> is Int) { (): Int -> <info descr="Automatically cast to kotlin.Int"><info>a</info></info> }
    else { () -> 1 }

fun merge<TYPO descr="Typo: In word 'Autocasts'">Autocasts</TYPO>(a: Any?) {
  if (a is String || a is Int) {
    a.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: compareTo">compareTo</error>("")
    <info descr="Automatically cast to kotlin.Any"><info>a</info></info>.toString()
  }
  if (a is Int || a is String) {
    a.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: compareTo">compareTo</error>("")
  }
  when (a) {
    is String, is Any -> a.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: compareTo">compareTo</error>("")
  }
  if (a is String && a is Any) {
    val <warning>i</warning>: Int = <info descr="Automatically cast to kotlin.String">a</info>.compareTo("")
  }
  if (a is String && <info descr="Automatically cast to kotlin.String">a</info>.compareTo("") == 0) {}
  if (a is String || a.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: compareTo">compareTo</error>("") <error>==</error> 0) {}
}

//mutability
fun f(): String {
    var <info>a</info>: Any = 11
    if (<info>a</info> is String) {
        val <warning>i</warning>: String = <error descr="[AUTOCAST_IMPOSSIBLE] Automatic cast to 'kotlin.String' is impossible, because 'a' could have changed since the is-check">a</error>
        <error descr="[AUTOCAST_IMPOSSIBLE] Automatic cast to 'kotlin.String' is impossible, because 'a' could have changed since the is-check">a</error>.compareTo("f")
        val <warning>f</warning>: Function0<String> = { <error descr="[AUTOCAST_IMPOSSIBLE] Automatic cast to 'kotlin.String' is impossible, because 'a' could have changed since the is-check">a</error> }
        return <error descr="[AUTOCAST_IMPOSSIBLE] Automatic cast to 'kotlin.String' is impossible, because 'a' could have changed since the is-check">a</error>
    }
    return ""
}

fun foo(aa: Any): Int {
    var a = aa
    if (a is Int) {
        return <error descr="[AUTOCAST_IMPOSSIBLE] Automatic cast to 'kotlin.Int' is impossible, because 'a' could have changed since the is-check">a</error>
    }
    return 1
}

fun inForLoop(x: Any?) {
    if (x is Array<String>) {
        for (i in <info descr="Automatically cast to kotlin.Array<kotlin.String>">x</info>) {}
    }
    for (i in <error descr="[ITERATOR_MISSING] For-loop range must have an iterator() method">x</error>) {}
}
