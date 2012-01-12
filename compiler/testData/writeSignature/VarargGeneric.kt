fun <P> foo(vararg tail: P) = 1

// method: namespace::foo
// jvm signature:     (Ljet/TypeInfo;[Ljava/lang/Object;)I
// generic signature: <P:Ljava/lang/Object;>(Ljet/TypeInfo;[TP;)I
// kotlin signature:  <P:?Ljava/lang/Object;>(null[TP;)I
// TODO: skip kotlin signature
// TODO: properly serialize typeinfo
