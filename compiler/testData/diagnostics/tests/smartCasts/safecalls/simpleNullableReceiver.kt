fun Any?.foo(my: My) = my === this

class My(val x: Any)

// my is nullable in brackets because Any?.foo has nullable receiver
fun foo(my: My?) = my?.x.foo(<!TYPE_MISMATCH!>my<!>)