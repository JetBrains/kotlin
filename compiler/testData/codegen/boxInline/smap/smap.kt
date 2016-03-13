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
inline fun head(init: () -> Unit) {val p = initTag2(init); return p}


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

//TODO SHOULD BE EMPTY
SMAP
1.kt
Kotlin
*S Kotlin
*F
+ 1 1.kt
builders/_1Kt
*L
1#1,21:1
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
18#2:26
6#2,9:27
10#2,3:36
7#2:39
*E
