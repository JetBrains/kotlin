package test

import a.*
import a.impl.*

val a1: A = A()
val a2: A = A.getInstance()
val a3: AImpl = A.getInstance()
val a4: String = AImpl.method()
val a5: String = AImpl.field

val k1: K = K()
val k2: K = K.getInstance()
val k3: KImpl = K.getInstance()
val k4: String = KImpl.method()
val k5: String = KImpl.field

val kf1: String = fileField
val kf2: String = fileMethod()
