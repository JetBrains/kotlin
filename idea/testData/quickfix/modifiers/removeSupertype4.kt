// "Remove supertype" "true"
open class C1 {}
open class C2 {}
class C3: C1(), /* Hello, world! */ C2<caret>() {}

/* IGNORE_FIR */