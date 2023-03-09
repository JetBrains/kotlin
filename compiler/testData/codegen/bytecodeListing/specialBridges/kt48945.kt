// TARGET_BACKEND: JVM
// FULL_JDK

// IGNORE_BACKEND_K2: JVM_IR
// FIR status: KT-57301 K2: `getOrDefault` and bridges are not generated for certain Map subclasses

// FILE: kt48945.kt
interface MSS : Map<String, String>
interface GM<K, V> : Map<K, V>
interface SMSS : GM<String, String>

class Test_MapStringString_AbstractMapStringString :
    Map<String, String>, java.util.AbstractMap<String, String>()
{
    override val entries: MutableSet<MutableMap.MutableEntry<String, String>>
        get() = TODO()
}

class Test_MapStringString_JMapFinalRemove :
    Map<String, String>, JMapFinalRemove()
{
    override val entries: MutableSet<MutableMap.MutableEntry<String, String>>
        get() = TODO()
}

class Test_MSS_AbstractMapStringString :
    MSS, java.util.AbstractMap<String, String>()
{
    override val entries: MutableSet<MutableMap.MutableEntry<String, String>>
        get() = TODO()
}

class Test_MSS_JMapFinalRemove :
    MSS, JMapFinalRemove()
{
    override val entries: MutableSet<MutableMap.MutableEntry<String, String>>
        get() = TODO()
}

class Test_GMStringString_AbstractMapStringString :
    GM<String, String>, java.util.AbstractMap<String, String>()
{
    override val entries: MutableSet<MutableMap.MutableEntry<String, String>>
        get() = TODO()
}

class Test_GMStringString_JMapFinalRemove :
    GM<String, String>, JMapFinalRemove()
{
    override val entries: MutableSet<MutableMap.MutableEntry<String, String>>
        get() = TODO()
}

class Test_SMSS_AbstractMapStringString :
    SMSS, java.util.AbstractMap<String, String>()
{
    override val entries: MutableSet<MutableMap.MutableEntry<String, String>>
        get() = TODO()
}

class Test_SMSS_JMapFinalRemove :
    SMSS, JMapFinalRemove()
{
    override val entries: MutableSet<MutableMap.MutableEntry<String, String>>
        get() = TODO()
}

// FILE: JMapFinalRemove.java
import java.util.AbstractMap;

public abstract class JMapFinalRemove extends AbstractMap<String, String> {
    @Override
    public final String remove(Object key) {
        return super.remove(key);
    }
}
