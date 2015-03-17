fun ()
fun T.()
fun T.(a : foo) : bar
fun T.<T : (a) -> b>(a : foo) : bar

fun ();
fun T.();
fun T.(a : foo) : bar;
fun T.<T : (a) -> b>(a : foo) : bar;

fun () {}
fun [a] T.() {}
fun [a] T.(a : foo) : bar {}
fun [a()] T.<T :  (a) -> b>(a : foo) : bar {}
fun [a()] T.<T : [a]  (a) -> b>(a : foo) : bar {}

fun A?.() : bar?
fun A? .() : bar?