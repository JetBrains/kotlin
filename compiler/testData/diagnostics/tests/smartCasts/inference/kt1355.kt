//KT-1355 Type inference fails with smartcast and generic function
//tests for Map.set
package a

import java.util.HashMap

fun foo(map: MutableMap<Int, String>, value: String?) {
    if (value != null) {
        map.put(1, <!DEBUG_INFO_SMARTCAST!>value<!>) //ok
        map.set(1, <!DEBUG_INFO_SMARTCAST!>value<!>) //type inference failed
        map[1] = <!DEBUG_INFO_SMARTCAST!>value<!>    //type inference failed
    }
}

//---------------------------

public data class Tag(public var tagName: String) {
    public val attributes: MutableMap<String, String> = HashMap<String, String>()
    public val contents: MutableList<Tag> = arrayListOf()

    public var id: String?
        get() = attributes["id"]
        set(value) {
            if(value == null) {
                attributes.remove("id")
            }
            else {
                attributes["id"] = value<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
                attributes["id"] = <!DEBUG_INFO_SMARTCAST!>value<!>
            }
        }
}


//from library
operator fun <K, V> MutableMap<K, V>.set(key : K, value : V) = this.put(key, value)

fun <T> arrayListOf(vararg <!UNUSED_PARAMETER!>values<!>: T): MutableList<T> = throw Exception()


