val t = context(a: A) fun () { }
val t = context(a: A) fun <A> () { }
val t = @Ann context(a: A) fun () { }
val t = context(a)