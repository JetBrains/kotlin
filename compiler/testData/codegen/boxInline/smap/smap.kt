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
1#1,21:1
10#1,3:22
6#1,2:25
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
18#2:26
6#2,9:27
10#2,3:36
7#2:39
18#2:43
6#2:44
14#2:46
10#2,2:47
12#2:51
7#2:53
*E