class Box<T>

fun <vararg Ts> simple(vararg args: *Ts) {}
fun <vararg Ts> boxed(vararg args: *Box<Ts>) {}
fun <R, vararg Ts> withTransform(
    transfrorm: (*Ts) -> R,
    vararg args: *Box<Ts>
) {}

// method: FunctionDeclarationsKt::simple
// jvm signature:     (Lkotlin/Tuple;)V
// generic signature: <Ts:Ljava/lang/Object;>(Lkotlin/Tuple<TTs;>;)V

// method: FunctionDeclarationsKt::boxed
// jvm signature:     (Lkotlin/Tuple;)V
// generic signature: <Ts:Ljava/lang/Object;>(Lkotlin/Tuple<LBox<TTs;>;>;)V

// method: FunctionDeclarationsKt::withTransform
// jvm signature:     (Lkotlin/jvm/functions/Function1;Lkotlin/Tuple;)V
// generic signature: <R:Ljava/lang/Object;Ts:Ljava/lang/Object;>(Lkotlin/jvm/functions/Function1<-Lkotlin/Tuple<TTs;>;+TR;>;Lkotlin/Tuple<LBox<TTs;>;>;)V
