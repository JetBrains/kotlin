class A() {}

open class B<T>() {
    fun isT (a : Any?) : Boolean {
        return a is T
    }
}

class C() : B<String>() {
}

class D<T>() : B<B<T>>() {
}

fun t1() : Boolean {
    val a = A()
    if(a !is A)   return false
    return true
}

fun t2() : Boolean {
    val a = A()
    if(a !is A?)   return false
    return true
}

fun t3() : Boolean {
    val a = A()
    if(null !is A?)   return false
    return true
}

fun t4 () : Boolean {
    val b = B<String>()
    if(b !is B<String>)   return false
    return true
}

fun t5 () : Boolean {
    val b = B<String>()
    if(b !is B<String>?)  return false
    return true
}

fun t6 () : Boolean {
    if(null !is B<String>?)  return false
    return true
}

fun t7 () : Boolean {
    val b = B<String>()
    if(!b.isT("aaa")) return false
    return true
}

fun t8 () : Boolean {
    val b = B<String>()
    if(b.isT(10))     return false
    return true
}

fun t9 () : Boolean {
    val b = B<String>()
    if(b.isT(null))   return false
    return true
}

fun t10 () : Boolean {
    val d = B<String?>()
    if(!d.isT("aaa")) return false
    return true
}
fun t11 () : Boolean {
    val d = B<String?>()
    if(d.isT(10))     return false
    return true
}

fun t12 () : Boolean {
    val d = B<String?>()
    if(!d.isT(null))  return false
    return true
}

fun t13 () : Boolean {
    val f = B<java.lang.String?>()
    if(!f.isT("aaa")) return false
    return true
}

fun t14 () : Boolean {
    val f = B<java.lang.String?>()
    if(f.isT(10))     return false
    return true
}

fun t15 () : Boolean {
    val f = B<java.lang.String?>()
    if(!f.isT(null))  return false
    return true
}

fun t16 () : Boolean {
    val c = B<Int>()
    if(c.isT("aaa")) return false
    return true
}

fun t17 () : Boolean {
    val c = B<Int>()
    if(!c.isT(10))   return false
    return true
}

fun t18 () : Boolean {
    val c = B<Int>()
    if(c.isT(null)) return false
    return true
}

fun t19 () : Boolean {
    val e = B<Int?>()
    if(e.isT("aaa")) return false
    return true
}

fun t20 () : Boolean {
    val e = B<Int?>()
    if(!e.isT(10))   return false
    return true
}

fun t21 () : Boolean {
    val e = B<Int?>()
    if(!e.isT(null)) return false
    return true
}

fun t22 () : Boolean {
    val b = B<String>()
    val w : B<String>? = b as B<String>   //ok
    val x = w as B<String>?  //TypeCastException
    return true
}

fun t23 () : Boolean {
    val b = B<String>()
    val v = b as B<String>   //ok
    return true
}

fun t24 () : Boolean {
    val b = B<String>()
    val u = b as B<String>?  //TypeCastException
    return true
}

fun t25 () : Boolean {
    val c = C()
    if(!c.isT("aaa")) return false
    return true
}

fun t26 () : Boolean {
    val d = D<String>()
    if(!d.isT(B<String>())) return false
    return true
}

fun box() : String {
    if(!t1()) {
        return "t1 failed"
    }
    if(!t2()) {
        return "t2 failed"
    }
    if(!t3()) {
        return "t3 failed"
    }
    if(!t4()) {
         return "t4 failed"
     }
     if(!t5()) {
         return "t5 failed"
     }
     if(!t6()) {
         return "t6 failed"
     }
     if(!t7()) {
         return "t7 failed"
     }
    if(!t8()) {
        return "t8 failed"
    }
    if(!t9()) {
        return "t9 failed"
    }
    if(!t10()) {
        return "t10 failed"
    }
    if(!t11()) {
         return "t11 failed"
     }
     if(!t12()) {
         return "t12 failed"
     }
     if(!t13()) {
         return "t13 failed"
     }
     if(!t14()) {
         return "t14 failed"
     }
    if(!t15()) {
        return "t15 failed"
    }
    if(!t16()) {
        return "t16 failed"
    }
    if(!t17()) {
        return "t17 failed"
    }
    if(!t18()) {
         return "t18 failed"
     }
     if(!t19()) {
         return "t19 failed"
     }
     if(!t20()) {
         return "t10 failed"
     }
     if(!t21()) {
         return "t21 failed"
     }
     if(!t22()) {
         return "t22 failed"
     }
     if(!t23()) {
         return "t23 failed"
     }
     if(!t24()) {
         return "t24 failed"
     }
     if(!t25()) {
         return "t25 failed"
     }
     if(!t26()) {
         return "t26 failed"
     }
     return "OK"
}