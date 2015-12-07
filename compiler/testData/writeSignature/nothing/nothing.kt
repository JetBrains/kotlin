class C<T>
class I<in A, B>
class Z<A, B>

fun f(
        p: Nothing, p1: C<Nothing>, p2: C<C<Nothing>>, p3: C<C<Nothing>>?,
        p4: I<Nothing, Int>, p5: C<I<Nothing, String>>, p6: C<in Nothing>,
        p7: Z<Nothing, String>, p8: Z<String, in Nothing>, p9: I<Nothing, Nothing>
): Nothing = throw Exception()

// method: NothingKt::f
// jvm signature: (Ljava/lang/Void;LC;LC;LC;LI;LC;LC;LZ;LZ;LI;)Ljava/lang/Void;
// generic signature: (Ljava/lang/Void;LC;LC<LC;>;LC<LC;>;LI<*Ljava/lang/Integer;>;LC<LI<*Ljava/lang/String;>;>;LC;LZ;LZ;LI;)Ljava/lang/Void;
