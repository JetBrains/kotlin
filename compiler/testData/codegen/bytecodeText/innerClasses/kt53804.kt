// OLD_INNER_CLASSES_LOGIC
// TARGET_BACKEND: JVM_IR

// FILE: classes.kt

public class A0 {
    public class B0 {
        public class C0 {
            public class D0
        }
    }
}

public class A1 {
    public class B1 {
        public class C1 {
            public class D1
        }
    }
}

public class A2 {
    public class B2 {
        public class C2 {
            public class D2
        }
    }
}

object A3 {
    interface B3 {
        interface C3 {
            interface D3
        }
    }
}

public class A4 {
    public class B4 {
        public class C4 {
            public class D4
        }
    }
}

public class A5 {
    public class B5 {
        public class C5 {
            public class D5
        }
    }
}

public class A6 {
    public class B6 {
        public class C6 {
            public class D6
        }
    }
}

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class ClassHolder(val value: kotlin.reflect.KClass<*>)

public class A7 {
    public class B7 {
        public class C7 {
            public class D7
        }
    }
}

@JvmInline
public value class A8(private val value: Int) {
    @JvmInline
    public value class B8(private val value: Int) {
        @JvmInline
        public value class C8(private val value: Int) {
            @JvmInline
            public value class D8(private val value: Int)
        }
    }
}


inline fun <reified T> foo() = null


// FILE: X.kt
class X {
    fun f0(t: List<Array<Array<Thread.State>>>) {}
    fun f1(): A1.B1.C1? = null
    fun f2() {
        foo<A2.B2.C2>()
    }
    fun f3(x: Any): Any? = x as? A3.B3.C3
    fun f4(): String {
        val x = listOf<A4.B4.C4>()
        return x.toString()
    }

    class Y {
        fun f5(): String {
            val x: A5.B5.C5? = null
            return x.toString()
        }
        fun f6(): A6.B6.C6? = null

        fun f7(@ClassHolder(A7.B7.C7::class) x: Int) {}

        fun f8() = A8.B8.C8::class.toString()
    }
}


// @X.class:
// 1 INNERCLASS
// 0 INNERCLASS java.lang.Thread\$State java.lang.Thread State
// 0 INNERCLASS A0\$B0 A0 B0
// 0 INNERCLASS A0\$B0\$C0 A0\$B0 C0
// 0 INNERCLASS A1\$B1 A1 B1
// 0 INNERCLASS A1\$B1\$C1 A1\$B1 C1
// 0 INNERCLASS A2\$B2 A2 B2
// 0 INNERCLASS A2\$B2\$C2 A2\$B2 C2
// 0 INNERCLASS A3\$B3 A3 B3
// 0 INNERCLASS A3\$B3\$C3 A3\$B3 C3
// 0 INNERCLASS A4\$B4 A4 B4
// 0 INNERCLASS A4\$B4\$C4 A4\$B4 C4
// 1 INNERCLASS X\$Y X Y

// @X$Y.class:
// 3 INNERCLASS
// 1 INNERCLASS X\$Y X Y
// 0 INNERCLASS A5\$B5 A5 B5
// 0 INNERCLASS A5\$B5\$C5 A5\$B5 C5
// 0 INNERCLASS A6\$B6 A6 B6
// 0 INNERCLASS A6\$B6\$C6 A6\$B6 C6
// 1 INNERCLASS A7\$B7 A7 B7
// 1 INNERCLASS A7\$B7\$C7 A7\$B7 C7
// 0 INNERCLASS A8\$B8 A8 B8
// 0 INNERCLASS A8\$B8\$C8 A8\$B8 C8
