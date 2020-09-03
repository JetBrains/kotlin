// FILE: rawTypeInSignature.kt
// WITH_SIGNATURES

class GenericInv<T : Number>
class GenericIn<in T : Number>
class GenericOut<out T : Number>

fun testReturnsRawGenericInv(j: JRaw) = j.returnsRawGenericInv()

fun testReturnsRawGenericIn(j: JRaw) = j.returnsRawGenericIn()

fun testReturnsRawGenericOut(j: JRaw) = j.returnsRawGenericOut()

class KRaw(j: JRaw) : JRaw by j
// JVM:     public <(Ljava/util/List<Ljava/lang/Object;>;)V> method takesRawList(p0: java.util.List): void
// JVM_IR:  public <(Ljava/util/List<+Ljava/lang/Object;>;)V> method takesRawList(p0: java.util.List): void

// FILE: JRaw.java

import java.util.*;

public interface JRaw {
    void takesRawList(List list);
    List returnsRawList();
    void takesRawGenericInv(GenericInv g);
    GenericInv returnsRawGenericInv();
    void takesRawGenericIn(GenericIn g);
    GenericIn returnsRawGenericIn();
    void takesRawGenericOut(GenericOut g);
    GenericOut returnsRawGenericOut();
}