// IGNORE_TREE_ACCESS: KT-64899
package util

@DestrAnno("destr 1 $prop")
val (@LeftAnno("a $prop") a, @RightAnno("b $prop") b) = 0 to 1

@Destr2Anno("destr 1 $prop")
val (@SecondLeftAnno("c $prop") c, @SecondRightAnno("d $prop") d) = 2 to 3