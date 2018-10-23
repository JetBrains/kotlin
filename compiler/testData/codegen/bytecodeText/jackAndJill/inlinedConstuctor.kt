public inline fun <T> Iterable(crossinline iterator: () -> Iterator<T>): Iterable<T> = object : Iterable<T> {
    override fun iterator(): Iterator<T> = iterator()
}

public fun IntArray.asIterable(): Iterable<Int> {
    return Iterable { this.iterator() }
}
/*Threre are two constuctors so we should be sure that we check required one by checking 'receiver$0$inlined' assign*/
// 1 <init>\(\[I\)V\s+L0\s+ALOAD 0\s+ALOAD 1\s+PUTFIELD InlinedConstuctorKt\$asIterable\$\$inlined\$Iterable\$1\.\$this_asIterable\$inlined : \[I
// 1 LOCALVARIABLE this LInlinedConstuctorKt\$asIterable\$\$inlined\$Iterable\$1; L0 L2 0