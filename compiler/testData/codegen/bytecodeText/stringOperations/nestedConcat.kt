fun test1(s1: String, s2: String, s3: String) =
        (s1 + s2) + s3

fun test2(s1: String, s2: String, s3: String) =
        s1 + (s2 + s3)

fun test3(s1: String, s2: String, s3: String, s4: String) =
        ((s1 + s2) + ((s3 + s4)))

fun test4(s1: String, s2: String, s3: String) =
        "s1: $s1; " +
        "s2: $s2; " +
        "s3: $s3"

// 4 NEW java/lang/StringBuilder