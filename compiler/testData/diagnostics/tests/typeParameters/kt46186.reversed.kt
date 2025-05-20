// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -FINAL_UPPER_BOUND -CAST_NEVER_SUCCEEDS
// LANGUAGE: -AllowEmptyIntersectionsInResultTypeResolver

interface I

class View1
open class View2
interface View3
abstract class View4
interface View5

fun <T: View1> findViewById1(): T = null as T
fun test1(): I = <!RETURN_TYPE_MISMATCH, TYPE_MISMATCH!>findViewById1()<!>

fun <T: View2> findViewById2(): T = null as T
fun test2(): I = findViewById2()

inline fun <reified T: View1> findViewById3(): T = null as T
fun test3(): I = <!RETURN_TYPE_MISMATCH, TYPE_MISMATCH!>findViewById3()<!>

inline fun <reified T: View2> findViewById4(): T = null as T
fun test4(): I = <!TYPE_INTERSECTION_AS_REIFIED_WARNING!>findViewById4<!>()

fun <T: View3> findViewById5(): T = null as T
fun test5(): I = findViewById5()

inline fun <reified T: View3> findViewById6(): T = null as T
fun test6(): I = <!TYPE_INTERSECTION_AS_REIFIED_WARNING!>findViewById6<!>()

fun <T: View4> findViewById7(): T = null as T
fun test7(): I = findViewById7()

inline fun <reified T: View4> findViewById8(): T = null as T
fun test8(): I = <!TYPE_INTERSECTION_AS_REIFIED_WARNING!>findViewById8<!>()

fun <T> findViewById9(): T where T: View3, T: View5 = null as T
fun test9(): I = findViewById9()

inline fun <reified T> findViewById10(): T where T: View3, T: View5 = null as T
fun test10(): I = <!TYPE_INTERSECTION_AS_REIFIED_WARNING!>findViewById10<!>()

fun <T: View2> findViewById11(): T = null as T
fun test11(): View4 = <!RETURN_TYPE_MISMATCH, TYPE_MISMATCH!>findViewById11()<!>

object Obj {
    fun <T: I> findViewById1(): T = null as T
    fun test1(): View1 = <!RETURN_TYPE_MISMATCH, TYPE_MISMATCH!>findViewById1()<!>

    fun <T: I> findViewById2(): T = null as T
    fun test2(): View2 = findViewById2()

    inline fun <reified T: I> findViewById3(): T = null as T
    fun test3(): View1 = <!RETURN_TYPE_MISMATCH, TYPE_MISMATCH!>findViewById3()<!>

    inline fun <reified T: I> findViewById4(): T = null as T
    fun test4(): View2 = <!TYPE_INTERSECTION_AS_REIFIED_WARNING!>findViewById4<!>()

    fun <T: I> findViewById5(): T = null as T
    fun test5(): View3 = findViewById5()

    inline fun <reified T: I> findViewById6(): T = null as T
    fun test6(): View3 = <!TYPE_INTERSECTION_AS_REIFIED_WARNING!>findViewById6<!>()

    fun <T: I> findViewById7(): T = null as T
    fun test7(): View4 = findViewById7()

    inline fun <reified T: I> findViewById8(): T = null as T
    fun test8(): View4 = <!TYPE_INTERSECTION_AS_REIFIED_WARNING!>findViewById8<!>()

    fun <T> findViewById9(): T where T: View3, T: View5 = null as T
    fun test9(): View1 = <!RETURN_TYPE_MISMATCH, TYPE_MISMATCH!>findViewById9()<!>

    inline fun <reified T> findViewById10(): T where T: View3, T: View5 = null as T
    fun test10(): View1 = <!INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION, TYPE_INTERSECTION_AS_REIFIED_WARNING!>findViewById10<!>()

    fun <T: View2> findViewById11(): T = null as T
    fun test11(): View4 = <!RETURN_TYPE_MISMATCH, TYPE_MISMATCH!>findViewById11()<!>
}

interface A
open class B {
    fun <T> f(): T where T : A, T : B = null as T
    fun g(): A = f()
}
