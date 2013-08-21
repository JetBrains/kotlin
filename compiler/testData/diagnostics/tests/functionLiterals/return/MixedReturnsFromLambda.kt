trait A
trait B: A
trait C: A


fun test(a: C, b: B) {
    (run @f{
      if (a != b) <!RETURN_NOT_ALLOWED_EXPLICIT_RETURN_TYPE_REQUIRED!>return@f a<!>
      b
    }): A
}

fun run<T>(f: () -> T): T { return f() }