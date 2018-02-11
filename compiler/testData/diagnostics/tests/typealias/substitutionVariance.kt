// NI_EXPECTED_FILE

class In<in T>
class Out<out T>
class Inv<T>

typealias In1<T> = In<T>
typealias In2<T> = In<<!REDUNDANT_PROJECTION!>in<!> T>
typealias In3<T> = In<<!CONFLICTING_PROJECTION!>out<!> T>
typealias In4<<!UNUSED_TYPEALIAS_PARAMETER!>T<!>> = In<*>

typealias Out1<T> = Out<T>
typealias Out2<T> = Out<<!CONFLICTING_PROJECTION!>in<!> T>
typealias Out3<T> = Out<<!REDUNDANT_PROJECTION!>out<!> T>
typealias Out4<<!UNUSED_TYPEALIAS_PARAMETER!>T<!>> = Out<*>

typealias Inv1<T> = Inv<T>
typealias Inv2<T> = Inv<in T>
typealias Inv3<T> = Inv<out T>
typealias Inv4<<!UNUSED_TYPEALIAS_PARAMETER!>T<!>> = Inv<*>

val inv1: Inv1<Int> = Inv<Int>()

fun inInv_Inv(x: In1<Int>) = x
fun inInv_In(x: In1<in Int>) = x
fun inInv_Out(x: <!CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION!>In1<out Int><!>) = x
fun inInv_Star(x: In1<*>) = x

fun inIn_Inv(x: In2<Int>) = x
fun inIn_In(x: In2<in Int>) = x
fun inIn_Out(x: <!CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION, CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION!>In2<out Int><!>) = x
fun inIn_Star(x: In2<*>) = x

fun inOut_Inv(x: <!CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION!>In3<Int><!>) = x
fun inOut_In(x: <!CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION!>In3<in Int><!>) = x
fun inOut_Out(x: <!CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION!>In3<out Int><!>) = x
fun inOut_Star(x: In3<*>) = x

fun outInv_Inv(x: Out1<Int>) = x
fun outInv_In(x: <!CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION!>Out1<in Int><!>) = x
fun outInv_Out(x: Out1<out Int>) = x
fun outInv_Star(x: Out1<*>) = x

fun outIn_Inv(x: <!CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION!>Out2<Int><!>) = x
fun outIn_In(x: <!CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION!>Out2<in Int><!>) = x
fun outIn_Out(x: <!CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION!>Out2<out Int><!>) = x
fun outIn_Star(x: Out2<*>) = x

fun outOut_Inv(x: Out3<Int>) = x
fun outOut_In(x: <!CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION, CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION!>Out3<in Int><!>) = x
fun outOut_Out(x: Out3<out Int>) = x
fun outOut_Star(x: Out3<*>) = x

fun invInv_Inv(x: Inv1<Int>) = x
fun invInv_In(x: Inv1<in Int>) = x
fun invInv_Out(x: Inv1<out Int>) = x
fun invInv_Star(x: Inv1<*>) = x

fun invIn_Inv(x: Inv2<Int>) = x
fun invIn_In(x: Inv2<in Int>) = x
fun invIn_Out(x: <!CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION!>Inv2<out Int><!>) = x
fun invIn_Star(x: Inv2<*>) = x

fun invOut_Inv(x: Inv3<Int>) = x
fun invOut_In(x: <!CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION!>Inv3<in Int><!>) = x
fun invOut_Out(x: Inv3<out Int>) = x
fun invOut_Star(x: Inv3<*>) = x
