import java.util.List
import java.lang.CharSequence

fun foo(p: List<CharSequence>) = 1

// method: namespace::foo
// jvm signature:     (Ljava/util/List;)I
// generic signature: (Ljava/util/List<Ljava/lang/CharSequence;>;)I
// kotlin signature:  (Ljava/util/List<Mjava/lang/CharSequence;>;)I // TODO: skip Kotlin signature
