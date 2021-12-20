package test

import java.io.Serializable

class TypeParams<in T1 : Any, out T2, T3 : (Int) -> Int, T4, T5 : Any?, T6 : T5, T7> where T1 : Cloneable?, T1 : Serializable, T2 : String, T7 : T6 {

    fun useParams(p1: T1, p2: (T2) -> Unit, p3: T3, p4: T4, P5: T5) {
    }

    fun useParamsInOtherOrder(p1: T3, p2: T3, p3: T1, p4: T5, P5: T1) {
    }

    fun useParamsInTypeArg(p1: List<T1>, p2: Map<T4?, T5?>, p3: (T4).(T2, T3) -> T1) {

    }

    fun <G1, G2, G3> withOwnParams(p1: G1, p2: G2, p3: G3, p4: T1, p5: (T2) -> Unit) {
    }

    fun <G1 : Any?, G2 : G1, G3, G4> withOwnParamsAndTypeConstraints(p1: G1, p2: G2, p3: G3, p4: T1, p5: (T2) -> Unit) where G4 : G1, G3 : String, G3 : Serializable? {
    }

    fun <T1, T2, T3> withOwnParamsClashing(p1: T1, p2: T2, p3: T3, p4: T4, p5: T5) {
    }

    fun <T1> T1.withOwnParamExtension(p: T1) {
    }

    val <G1> G1.withOwnParam: G1
        get() = throw IllegalStateException()

    val <G1: Int?> G1.withOwnBoundedParam: G1
        get() = throw IllegalStateException()

    val <G1: T4> G1.withOwnBoundedParamByOther: G1
        get() = throw IllegalStateException()

    val useSomeParam: T2
        get() = throw IllegalStateException()


    public inline fun <reified G, reified T> f(g: G, body: (G)-> T): T = body(g)

}