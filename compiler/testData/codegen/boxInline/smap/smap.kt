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
7#1,3:40
10#1:45
11#1,2:49
13#1:52
15#1:54
19#2:26
7#2,9:27
11#2,3:36
8#2:39
19#2:43
7#2:44
15#2:46
11#2,2:47
13#2:51
8#2:53
*E
*S KotlinDebug
*F
+ 1 2.kt
_2Kt
*L
20#1,3:40
20#1:45
20#1,2:49
20#1:52
20#1:54
9#1:26
9#1,9:27
9#1,3:36
9#1:39
20#1:43
20#1:44
20#1:46
20#1,2:47
20#1:51
20#1:53
*E