fun <P> foo(vararg tail: P) = 1

// method: namespace::foo
// jvm signature:     ([Ljava/lang/Object;)I
// generic signature: <P:Ljava/lang/Object;>([TP;)I
// kotlin signature:  <P:?Ljava/lang/Object;>([TP;)I
// TODO: skip kotlin signature
// TODO: properly serialize typeinfo
