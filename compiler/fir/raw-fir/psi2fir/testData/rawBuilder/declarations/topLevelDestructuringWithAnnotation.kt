package util

@Destructuring("destr $prop")
val (@LeftAnno("a $prop") a, @RightAnno("b $prop") b) = Pair(0, 1)