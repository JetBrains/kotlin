// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocks +CompanionExtensions
class Outer<T> {
    inner class Inner {
        companion {
            val innerProp: <!OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner<!>
                get() = <!INNER_CLASS_CONSTRUCTOR_NO_RECEIVER!>Inner<!>()

            fun foo(x: <!OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner<!>) {
                val x: <!OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner<!> = <!INNER_CLASS_CONSTRUCTOR_NO_RECEIVER!>Inner<!>()
            }
        }
    }

    companion {
         val innerProp: <!OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner<!>
             get() = <!INNER_CLASS_CONSTRUCTOR_NO_RECEIVER!>Inner<!>()
    }

    class Static {
         companion {
             val innerProp: <!OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner<!>
                 get() = <!INNER_CLASS_CONSTRUCTOR_NO_RECEIVER!>Inner<!>()
        }
    }

    inner class Mid { inner class Deep {} }

    companion {
        val p1: <!OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner?<!> = null                 // ERR
        val p2: <!OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner?<!> get() = null           // ERR
        lateinit var p3: <!OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner<!>                // ERR
        val p4: List<<!OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner<!>>? = null           // ERR
        val p5: (<!OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner<!>) -> Unit = <!OUTER_CLASS_ARGUMENTS_REQUIRED!>{}<!>          // ERR
        val p6: <!OUTER_CLASS_ARGUMENTS_REQUIRED!>Mid.Deep?<!> = null              // ERR
        fun f1(): <!OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner?<!> = null               // ERR
        fun f2(i: <!OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner<!>) {}                   // ERR
        fun f3(vararg i: <!OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner<!>) {}            // ERR
        fun <S : <!OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner<!>> f4() {}               // ERR
    }
}

class Outer2<T> {
    inner class Inner { fun foo() {} }
    companion {
        fun g1() { val x: <!OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner?<!> = null }               // ERR
        fun g2() = { i: <!OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner<!> -> }                      // ERR
        fun g3() = object { val x: <!OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner?<!> = null }      // ERR
        fun g4() { class L { val x: <!OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner?<!> = null } }   // ERR
        fun g5() { fun h(): <!OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner?<!> = null }             // ERR
        val r = <!OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner<!>::foo                              // ERR, reported on 2.4.21, missing since 2.5.0-Beta1
    }
}

class Outer3 { fun o() = 1; inner class Inner { companion { fun f() = <!UNRESOLVED_REFERENCE!>o<!>() } } }            // ERR, crash
class Outer4 { val o = 1; inner class Inner { companion { fun f() = <!UNRESOLVED_REFERENCE!>o<!> } } }                // ERR, crash
class Outer5 { val o = 1; inner class Inner { companion { val x get() = <!UNRESOLVED_REFERENCE!>o<!> } } }            // ERR, crash
class Outer6 { val o = 1; inner class Inner { companion { val x = <!UNRESOLVED_REFERENCE!>o<!> } } }                  // ERR, crash in <clinit>
class Outer7 { fun o() = 1; inner class Inner { companion { val r = ::<!UNRESOLVED_REFERENCE!>o<!> } } }              // ERR, crash in <clinit>
class Outer8 { fun o() = 1; inner class Inner { companion { fun f() = <!CANNOT_INFER_PARAMETER_TYPE!>run<!> { <!UNRESOLVED_REFERENCE!>o<!>() } } } }    // ERR, crash
class Outer9 { inner class Inner { companion { fun f() = this<!UNRESOLVED_LABEL!>@Outer<!> } } }                  // ERR, crash
class Outer10 { inner class Inner { companion { fun f() = <!NO_THIS!>this<!> } } }                        // ERR, crash, `this` is Outer
class Outer11 { inner class Inner { companion { fun f() = <!SUPER_NOT_AVAILABLE!>super@Outer<!>.hashCode() } } }      // ERR, crash
class Outer12 { inner class Other {}; inner class Inner { companion { fun f() = <!INNER_CLASS_CONSTRUCTOR_NO_RECEIVER!>Other<!>() } } } // ERR, crash
class Outer13<T>(val t: T) { inner class Inner { companion { fun f() = <!UNRESOLVED_REFERENCE!>t<!> } } }              // ERR, crash
class Outer14 { fun Int.ext() = 1; inner class Inner { companion { fun f() = 1.<!UNRESOLVED_REFERENCE!>ext<!>() } } }  // ERR, crash

open class Base { fun b() = 1 }
class Outer15 : Base() { inner class Inner { companion { fun f() = <!UNRESOLVED_REFERENCE!>b<!>() } } }   // ERR, crash

enum class E { A; inner class I { companion { fun f() = <!UNRESOLVED_REFERENCE!>name<!> } } }   // ERR, crash
class Outer16 { fun o() = 1; inner class Mid { fun m() = 2; inner class Inner { companion { fun f() = <!UNRESOLVED_REFERENCE!>o<!>() + <!UNRESOLVED_REFERENCE!>m<!>() } } } }   // ERR, crash
class Outer17 { inner class Mid { inner class Inner { companion { fun f() = <!INNER_CLASS_CONSTRUCTOR_NO_RECEIVER!>Mid<!>() } } } }   // ERR, crash
class Outer18 { fun o() = 1; fun m() = object { inner class I { <!ILLEGAL_COMPANION_BLOCK!>companion<!> { fun f() = <!UNRESOLVED_REFERENCE!>o<!>() } } } }   // ERR, crash

class Outer19 { fun o() = 1; fun m() { class L { <!ILLEGAL_COMPANION_BLOCK!>companion<!> { fun f() = <!UNRESOLVED_REFERENCE!>o<!>() } } } }   // ERR, crash in lowering: No dispatch receiver parameter for FUN f [companion]
fun g(): Int { val v = 1; class L { <!ILLEGAL_COMPANION_BLOCK!>companion<!> { fun f() = v } }; return L.f() }    // ERR, same lowering crash, also on JS and Wasm

// must keep compiling (OK)
class Outer20<T> { inner class Inner {}; companion { val p: Outer20<String>.Inner? = null; fun f() = Outer20<String>().Inner() } }
class Outer21<T> { inner class Inner {}; companion { val p: Outer21<*>.Inner? = null } }
class Outer22<T> { inner class Inner {}; companion { fun <S> f(o: Outer22<S>): Outer22<S>.Inner = o.Inner() } }
class Outer23<T> { inner class Inner {}; companion { val k = Inner::class } }
class Outer24<T> { class Nested {}; companion { val p: Nested? = null; fun f() = Nested() } }
class Outer25 { inner class Inner {}; companion { val p: Inner? = null; fun f(o: Outer25) = o.Inner() } }
class Outer26 { companion { fun s() = "OK" }; inner class Inner { companion { fun f() = s() } } }      // Outer.Inner.f() == "OK"

class Outer27 {
    companion {
        fun local() {
            class Outer<T> {
                inner class Inner
                val p: Inner? = null
            }
        }
    }
}

// nice to have
open class Outer28<T> {
    inner class Inner()
    companion {
        fun foo() {
            class U : Outer28<String>() {
                val p: <!OUTER_CLASS_ARGUMENTS_REQUIRED!>Inner<!> = Inner()
            }
        }
    }
}
/* GENERATED_FIR_TAGS: additiveExpression, anonymousObjectExpression, callableReference, classDeclaration,
classReference, enumDeclaration, enumEntry, funWithExtensionReceiver, functionDeclaration, functionalType, getter, inner,
integerLiteral, lambdaLiteral, lateinit, localClass, localFunction, localProperty, nestedClass, nullableType,
primaryConstructor, propertyDeclaration, starProjection, stringLiteral, thisExpression, typeConstraint, typeParameter,
vararg */
