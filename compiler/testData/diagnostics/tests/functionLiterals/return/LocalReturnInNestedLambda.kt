fun test() {
    (run @f{
      run @ff {
        <!RETURN_NOT_ALLOWED_EXPLICIT_RETURN_TYPE_REQUIRED!>return@ff "2"<!>
      }
      <!RETURN_NOT_ALLOWED_EXPLICIT_RETURN_TYPE_REQUIRED!>return@f 1<!>
    }): Int
}

fun run<T>(f: () -> T): T { return f() }