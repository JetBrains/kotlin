// TARGET_BACKEND: JVM_IR

fun test(a: Int, b: Int, flag: Boolean) =
    (if (flag) a..b else a downTo b).map { it + 1 }

// 0 java/util/Iterator.next \(\)Ljava/lang/Object;
// 1 kotlin/collections/IntIterator.nextInt \(\)I
