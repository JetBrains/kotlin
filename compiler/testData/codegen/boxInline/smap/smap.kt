// This test depends on line numbers
// FILE: 1.kt

package builders

inline fun init(init: () -> Unit) {
    init()
}

inline fun initTag2(init: () -> Unit) {
    val p = 1;
    init()
}
//{val p = initTag2(init); return p} to remove difference in linenumber processing through MethodNode and MethodVisitor should be: = initTag2(init)
inline fun head(init: () -> Unit) { val p = initTag2(init); return p}


inline fun html(init: () -> Unit) {
    return init(init)
}

// FILE: 2.kt

import builders.*


inline fun test(): String {
    var res = "Fail"

    html {
        head {
            res = "OK"
        }
    }

    return res
}


fun box(): String {
    var expected = test();

    return expected
}

// FILE: 1.smap

SMAP
1.kt
Kotlin
*S Kotlin
*F
+ 1 1.kt
builders/_1Kt
*L
1#1,22:1
11#1,3:23
7#1,2:26
*E
*S KotlinDebug
*F
+ 1 1.kt
builders/_1Kt
*L
15#1,3:23
19#1,2:26
*E

// FILE: 2.smap


SMAP
2.kt
Kotlin
*S Kotlin
*F
+ 1 2.kt
_2Kt
+ 2 1.kt
builders/_1Kt
*L
1#1,25:1
7#1,3:36
10#1:41
11#1,2:45
13#1:48
15#1:50
19#2:26
7#2,9:27
19#2:39
7#2:40
15#2:42
11#2,2:43
13#2:47
8#2:49
*E
*S KotlinDebug
*F
+ 1 2.kt
_2Kt
*L
20#1,3:36
20#1:41
20#1,2:45
20#1:48
20#1:50
9#1:26
9#1,9:27
20#1:39
20#1:40
20#1:42
20#1,2:43
20#1:47
20#1:49
*E