val a = fun ()
val a = fun name()
val a = fun T.name()
val a = fun [a] T.(a : foo) : bar
val a = fun [a] T.name(a : foo) : bar
val a = fun [a()] T.<T : (a) -> b>(a : foo) : bar

fun c() = fun ();
fun c() = fun name();
fun c() = fun [a] T.();
fun c() = fun [a] T.(a : foo) : bar;
fun c() = fun [a()] T.<T : (a) -> b>(a : foo) : bar;

val d = fun () = a
val d = fun name() = a
val a = [a] fun ()

val b = fun <T> () where T: A

fun outer() {
    bar(fun () {})
    bar(fun name() {})
    bar(fun [a] T.() {})
    bar(fun [a] T.name() {})

    bar(fun [a] T.(a : foo) : bar {})
    bar(fun [a()] T.<T :  (a) -> b>(a : foo) : bar {})

    bar {fun [a()] T.<T : [a]  (a) -> b>(a : foo) : bar {}}

    bar {fun A?.() : bar?}
    bar {fun A? .() : bar?}

    bar(fun () = a)
    bar(fun name() = a)
    bar([a] fun name() = a)
}
