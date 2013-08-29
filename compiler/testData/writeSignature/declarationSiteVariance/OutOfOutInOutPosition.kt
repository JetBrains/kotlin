class Out<out T>
class X

// Why we want this to be translated to 'Out<Out<? extends X>> f()' instead of 'Out<Out<X>> f()'
// There are two instantiations of 'In' in this test: outer Out<...> and inner Out<X>
// So why do we want to put a wildcard on the inner one and not the outer?
// People don't want wildcards in return types, because they are _long_. So we try our best to remove wildcards where possible
// Not putting a wildcard on the outer occurrence is not imposing a restriction, actually it is removing one:
//     anything that can be done with Out<? extends X> in Java can be done with Out<X>
// But omitting the wildcard on the inner occurrence is restrictive:
//     one can add a List<String> to a List<List<? extends CharSequence>>,
//     but not to a List<List<CharSequence>>,
//     thus removing the wildcard would be restricting the use of the return value of the method, and we don't want do this.
fun f(): Out<Out<X>> = throw Exception()

// method: _DefaultPackage::f
// jvm signature:     ()LOut;
// generic signature: ()LOut<LOut<+LX;>;>;